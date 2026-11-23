package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.dto.ConviteDadosResponse;
import com.cursovalido.backend.authentication.dto.ConviteResumoResponse;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.exception.ServicoEmailIndisponivelException;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.authentication.validation.convite.ConviteValidation;
import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConviteAcessoService {
    private static final StatusConvite PENDENTE = StatusConvite.PENDENTE;
    private final ConviteAcessoRepository convites;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificadorSenha;
    private final PoliticaService politicas;
    private final ResponsabilidadeService responsabilidade;
    private final EmailService emails;
    private final ValidacaoEmailService validacaoEmail;
    private final ContaUsuarioService contas;
    private final LogAuditoriaService auditoria;
    private final UsuarioForumService usuariosForum;
    private final PrivilegiosAdministrativosService privilegios;
    private final TokenEmailService tokensEmail;
    private final SecureRandom aleatorio = new SecureRandom();

    @Transactional
    // Cria um convite de acesso sem duplicar convites pendentes.
    public void criar(String email, Perfil perfil, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        validacaoEmail.validar(email);
        String endereco = email.trim().toLowerCase(Locale.ROOT);
        ConviteValidation.validarCriacao(endereco, perfil, usuarios);
        ConviteValidation.validarLimiteDoAdministrador(administradorId, convites);
        ConviteAcesso pendente = convites.findFirstByEmailAndStatusOrderByIdDesc(endereco, PENDENTE).orElse(null);
        ConviteValidation.validarConvitePendente(pendente);
        if (pendente != null) {
            pendente.setStatus(StatusConvite.CANCELADO);
            convites.saveAndFlush(pendente);
        }
        String token = gerarToken();
        ConviteAcesso convite = new ConviteAcesso(endereco, perfil, hash(token), LocalDateTime.now().plusHours(48));
        convite.setCriadoPor(administradorId);
        try {
            // Impede convites pendentes duplicados pelo índice único.
            convites.saveAndFlush(convite);
        } catch (DataIntegrityViolationException excecao) {
            throw new ConviteAcessoInvalidoException(HttpStatus.CONFLICT, "CONVITE_JA_PENDENTE",
                    "Já existe um convite pendente para este e-mail. Use a opção Reenviar ou cancele o convite atual.");
        }
        emails.enviarConvite(convite, token);
        auditoria.registrar(administradorId, "CONVITE_ACESSO_CRIADO", endereco, null);
    }

    @Transactional
    public ConviteDadosResponse consultar(String token) {
        ConviteAcesso convite = localizar(token);
        ConviteValidation.validarPrazo(convite);
        return new ConviteDadosResponse(convite.getEmail(), convite.getPerfil());
    }

    @Transactional
    // Conclui o cadastro pelo convite e libera o acesso.
    public void completar(String token, CadastroUsuarioRequest cadastro) {
        completar(token, cadastro, null);
    }

    @Transactional
    public void completar(String token, CadastroUsuarioRequest cadastro, String enderecoIp) {
        validacaoEmail.validar(cadastro.email());
        ConviteValidation.validarMaioridadeDeclarada(cadastro);
        ConviteAcesso convite = localizar(token);
        ConviteValidation.validarCadastro(convite, cadastro, politicas, usuarios);

        responsabilidade.validar(convite.getPerfil(), cadastro.aceitouResponsabilidade(),
                cadastro.versaoResponsabilidade(), enderecoIp);
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(cadastro.nomeCompleto());
        usuario.setEmail(convite.getEmail());
        usuario.setSenha(codificadorSenha.encode(cadastro.senha()));
        usuario.setMatricula(gerarMatricula());
        usuario.setDeclarouMaiorIdade(true);
        usuario.setAceitouTermosLgpd(true);
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos(cadastro.versaoTermos());
        usuario.setPerfil(convite.getPerfil());
        usuario.setDoisFatoresAtivos(convite.getPerfil().equipe());
        usuario.setEmailConfirmado(true);
        usuario.setAtivo(true);
        usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
        usuarios.saveAndFlush(usuario);
        responsabilidade.registrar(usuario, cadastro.aceitouResponsabilidade(),
                cadastro.versaoResponsabilidade(), enderecoIp);
        convite.concluir();
        convites.save(convite);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(usuario.getId(), "CADASTRO_POR_CONVITE", convite.getPerfil().name(), enderecoIp);
    }

    // Lista os usuários que aguardam aprovação.
    public List<Usuario> pendentes() {
        return usuarios.findAll().stream()
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.PENDENTE_APROVACAO)
                .toList();
    }

    public List<Usuario> pendentes(Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        return pendentes();
    }

    // Lista os convites que ainda estão pendentes.
    public List<ConviteResumoResponse> convitesPendentes() {
        return convites.findByStatusOrderByCriadoEmAsc(PENDENTE).stream()
                .map(convite -> new ConviteResumoResponse(convite.getId(), convite.getEmail(), convite.getPerfil(),
                        convite.getStatus(), convite.isEmailConfirmado(), convite.getCriadoEm(), convite.getExpiraEm()))
                .toList();
    }

    public List<ConviteResumoResponse> convitesPendentes(Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        return convitesPendentes();
    }

    @Transactional
    // Aprova o acesso de um usuário pendente.
    public void aprovar(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
        usuario.setAtivo(true);
        usuario.setEmailConfirmado(true);
        usuarios.save(usuario);
        tokensEmail.invalidarAcessos(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(administradorId, "ACESSO_APROVADO", "Usuario " + idUsuario, null);
        try {
            emails.enviarAcessoAprovado(usuario);
        } catch (ServicoEmailIndisponivelException excecao) {
            // Mantém a aprovação se o envio do aviso falhar.
            auditoria.registrar(administradorId, "AVISO_APROVACAO_NAO_ENVIADO", "Usuario " + idUsuario, null);
        }
    }

    @Transactional
    // Rejeita o cadastro de um usuário pendente.
    public void rejeitar(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        auditoria.registrar(administradorId, "ACESSO_REJEITADO", "Usuario " + idUsuario, null);
        contas.excluirPendencia(usuario.getId());
    }

    @Transactional
    // Remove uma pendência de cadastro.
    public void remover(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        contas.excluirPendencia(usuario.getId());
        auditoria.registrar(administradorId, "PENDENCIA_REMOVIDA", "Usuario " + idUsuario, null);
    }

    @Transactional
    // Reenvia um convite que ainda esta pendente.
    public void reenviar(Long idConvite, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        ConviteAcesso convite = convites.bloquearPorId(idConvite)
                .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado."));
        ConviteValidation.validarReenvio(convite);
        String token = gerarToken();
        convite.renovar(hash(token), LocalDateTime.now().plusHours(48));
        convites.save(convite);
        emails.enviarConvite(convite, token);
        auditoria.registrar(administradorId, "CONVITE_REENVIADO", convite.getEmail(), null);
    }

    @Transactional
    // Cancela um convite existente.
    public void cancelarConvite(Long idConvite, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        ConviteAcesso convite = convites.bloquearPorId(idConvite)
                .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado."));
        ConviteValidation.validarPendente(convite);
        convite.setStatus(StatusConvite.CANCELADO);
        convites.save(convite);
        auditoria.registrar(administradorId, "CONVITE_REMOVIDO", convite.getEmail(), null);
    }

    private Usuario usuarioPendente(Long id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro pendente não encontrado."));
        ConviteValidation.validarUsuarioPendente(usuario);
        return usuario;
    }

    private ConviteAcesso localizar(String token) {
        ConviteAcesso convite = convites.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("O link do convite é inválido. Peça um novo convite à administração."));
        ConviteValidation.validarPendente(convite);
        return convite;
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception excecao) {
            throw new IllegalStateException("Nao foi possivel proteger o convite", excecao);
        }
    }

    private String gerarMatricula() {
        String matricula;
        do {
            matricula = Integer.toString(10000000 + aleatorio.nextInt(90000000));
        } while (usuarios.existsByMatricula(matricula));
        return matricula;
    }
}
