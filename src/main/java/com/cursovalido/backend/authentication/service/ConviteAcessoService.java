package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.dto.ConviteDadosResponse;
import com.cursovalido.backend.authentication.dto.ConviteResumoResponse;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
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
    private final LimiteCadastroService limiteCadastro;
    private final SecureRandom aleatorio = new SecureRandom();

    @Transactional
    // Cria um convite de acesso para um novo usuario.
    public void criar(String email, Perfil perfil, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        validacaoEmail.validar(email);
        String endereco = email.trim().toLowerCase(Locale.ROOT);
        ConviteValidation.validarCriacao(endereco, perfil, usuarios);
        String token = gerarToken();
        convites.findByEmail(endereco).filter(anterior -> anterior.getStatus() == PENDENTE)
                .ifPresent(anterior -> anterior.setStatus(StatusConvite.CANCELADO));
        ConviteAcesso convite = new ConviteAcesso(endereco, perfil, hash(token), LocalDateTime.now().plusHours(48));
        convite.setCriadoPor(administradorId);
        convites.saveAndFlush(convite);
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
    // Confirma o e-mail usado no convite.
    public void confirmarEmail(String token) {
        ConviteAcesso convite = localizar(token);
        ConviteValidation.validarPrazo(convite);
        convite.setEmailConfirmado(true);
        convites.save(convite);
    }

    @Transactional
    // Completa o cadastro e deixa o usuario aguardando aprovacao.
    public void completar(String token, CadastroUsuarioRequest cadastro) {
        completar(token, cadastro, null);
    }

    @Transactional
    public void completar(String token, CadastroUsuarioRequest cadastro, String enderecoIp) {
        validacaoEmail.validar(cadastro.email());
        ConviteValidation.validarIdade(cadastro);
        ConviteAcesso convite = localizar(token);
        ConviteValidation.validarCadastro(convite, cadastro, politicas, usuarios);

        responsabilidade.validar(convite.getPerfil(), cadastro.aceitouResponsabilidade(),
                cadastro.versaoResponsabilidade(), enderecoIp);
        limiteCadastro.reservar(enderecoIp);
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(cadastro.nomeCompleto());
        usuario.setEmail(convite.getEmail());
        usuario.setSenha(codificadorSenha.encode(cadastro.senha()));
        usuario.setDataNascimento(cadastro.dataNascimento());
        usuario.setMatricula(gerarMatricula());
        usuario.setDeclarouMaiorIdade(cadastro.declarouMaiorIdade());
        usuario.setAceitouTermosLgpd(true);
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos(cadastro.versaoTermos());
        usuario.setPerfil(convite.getPerfil());
        usuario.setEmailConfirmado(true);
        usuario.setAtivo(false);
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        usuarios.save(usuario);
        responsabilidade.registrar(usuario, cadastro.aceitouResponsabilidade(),
                cadastro.versaoResponsabilidade(), enderecoIp);
        convite.setStatus(StatusConvite.CONCLUIDO);
        convites.save(convite);
        auditoria.registrar(usuario.getId(), "CADASTRO_PENDENTE_APROVACAO", convite.getPerfil().name(), enderecoIp);
    }

    // Lista os usuarios que aguardam aprovacao.
    public List<Usuario> pendentes() {
        return usuarios.findAll().stream()
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.PENDENTE_APROVACAO)
                .toList();
    }

    // Lista os convites que ainda estao pendentes.
    public List<ConviteResumoResponse> convitesPendentes() {
        return convites.findByStatusOrderByCriadoEmAsc(PENDENTE).stream()
                .map(convite -> new ConviteResumoResponse(convite.getId(), convite.getEmail(), convite.getPerfil(),
                        convite.getStatus(), convite.isEmailConfirmado(), convite.getCriadoEm(), convite.getExpiraEm()))
                .toList();
    }

    @Transactional
    // Aprova o acesso de um usuario pendente.
    public void aprovar(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
        usuario.setAtivo(true);
        usuarios.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(administradorId, "ACESSO_APROVADO", "Usuario " + idUsuario, null);
    }

    @Transactional
    // Rejeita o cadastro de um usuario pendente.
    public void rejeitar(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        auditoria.registrar(administradorId, "ACESSO_REJEITADO", "Usuario " + idUsuario, null);
        contas.excluirPendencia(usuario.getId());
    }

    @Transactional
    // Remove uma pendencia de cadastro.
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
        ConviteAcesso convite = convites.findById(idConvite)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        ConviteValidation.validarPendente(convite);
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
        ConviteAcesso convite = convites.findById(idConvite)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        convite.setStatus(StatusConvite.CANCELADO);
        convites.save(convite);
        auditoria.registrar(administradorId, "CONVITE_REMOVIDO", convite.getEmail(), null);
    }

    private Usuario usuarioPendente(Long id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pendencia nao encontrada"));
        ConviteValidation.validarUsuarioPendente(usuario);
        return usuario;
    }

    private ConviteAcesso localizar(String token) {
        ConviteAcesso convite = convites.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
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
