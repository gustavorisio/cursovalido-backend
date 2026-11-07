package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequisicao;
import com.cursovalido.backend.authentication.dto.ConviteResumoResposta;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Endereco;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import org.springframework.http.HttpStatus;
import java.util.Locale;
import com.cursovalido.backend.authentication.repository.ConviteAcessoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
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
    private final EmailService emails;
    private final ValidacaoEmailService validacaoEmail;
    private final IdentidadeExcluidaService identidades;
    private final LogAuditoriaService auditoria;
    private final UsuarioForumServico usuariosForum;
    private final PrivilegiosAdministrativosService privilegios;
    private final SecureRandom aleatorio = new SecureRandom();

    @Transactional
    // Cria um convite de acesso para um novo usuario.
    public void criar(String email, Perfil perfil, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        validacaoEmail.validar(email);
        String endereco = email.trim().toLowerCase(Locale.ROOT);
        if (perfil == Perfil.ALUNO) {
            throw new ConviteAcessoInvalidoException(HttpStatus.BAD_REQUEST, "PERFIL_CONVITE_INVALIDO",
                    "Convites sao permitidos apenas para professores e administradores.");
        }
        if (usuarios.existsByEmail(endereco)) {
            throw new ConviteAcessoInvalidoException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO",
                    "Este e-mail ainda possui uma conta cadastrada. A exclusao deve ser concluida antes de enviar outro convite.");
        }
        String token = gerarToken();
        ConviteAcesso convite = convites.findByEmail(endereco).orElse(null);
        if (convite == null) {
            convite = new ConviteAcesso(endereco, perfil, hash(token), LocalDateTime.now().plusHours(48));
        } else {
            convite.prepararNovoConvite(perfil, hash(token), LocalDateTime.now().plusHours(48));
        }
        convites.saveAndFlush(convite);
        emails.enviarConvite(convite, token);
        auditoria.registrar(administradorId, "CONVITE_ACESSO_CRIADO", endereco, null);
    }

    @Transactional
    // Confirma o e-mail usado no convite.
    public void confirmarEmail(String token) {
        ConviteAcesso convite = localizar(token);
        if (expirado(convite))
            throw new IllegalArgumentException("Convite expirado");
        convite.setEmailConfirmado(true);
        convites.save(convite);
    }

    @Transactional
    // Completa o cadastro e deixa o usuario aguardando aprovacao.
    public void completar(String token, CadastroUsuarioRequisicao cadastro) {
        validacaoEmail.validar(cadastro.email());
        identidades.verificarCadastro(cadastro.email(), cadastro.cpf());
        if (Period.between(cadastro.dataNascimento(), LocalDate.now()).getYears() < 18)
            throw new MenorDeIdadeException();
        if (!Boolean.TRUE.equals(cadastro.declarouMaiorIdade()))
            throw new MenorDeIdadeException();
        ConviteAcesso convite = localizar(token);
        if (expirado(convite))
            throw new IllegalArgumentException("Convite expirado");
        if (!convite.isEmailConfirmado())
            throw new IllegalArgumentException("E-mail ainda nao confirmado");
        if (!convite.getEmail().equalsIgnoreCase(cadastro.email())) {
            throw new IllegalArgumentException("E-mail diferente do convite");
        }
        if (!politicas.versaoAtiva(cadastro.versaoTermos()))
            throw new IllegalArgumentException("Versao dos termos invalida");
        if (!Boolean.TRUE.equals(cadastro.aceitouTermosLgpd()))
            throw new IllegalArgumentException("Termos nao aceitos");
        if (usuarios.existsByEmail(convite.getEmail())) {
            throw new IllegalArgumentException("Dados ja utilizados");
        }
        if (usuarios.existsByCpf(cadastro.cpf())) {
            throw new IllegalArgumentException("Dados ja utilizados");
        }

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(cadastro.nomeCompleto());
        usuario.setEmail(convite.getEmail());
        usuario.setSenha(codificadorSenha.encode(cadastro.senha()));
        usuario.setCpf(cadastro.cpf());
        usuario.setDataNascimento(cadastro.dataNascimento());
        usuario.setTelefone(cadastro.telefone());
        usuario.setMatricula(gerarMatricula());
        var endereco = cadastro.endereco();
        usuario.setEndereco(new Endereco(endereco.cep(), endereco.logradouro(), endereco.bairro(), endereco.cidade(),
                endereco.estado()));
        usuario.setDeclarouMaiorIdade(cadastro.declarouMaiorIdade());
        usuario.setAceitouTermosLgpd(true);
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos(cadastro.versaoTermos());
        usuario.setPerfil(convite.getPerfil());
        usuario.setEmailConfirmado(true);
        usuario.setAtivo(false);
        usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
        usuarios.save(usuario);
        convite.setStatus(StatusConvite.CONCLUIDO);
        convites.save(convite);
        auditoria.registrar(usuario.getId(), "CADASTRO_PENDENTE_APROVACAO", convite.getPerfil().name(), null);
    }

    // Lista os usuarios que aguardam aprovacao.
    public List<Usuario> pendentes() {
        return usuarios.findAll().stream()
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.PENDENTE_APROVACAO)
                .toList();
    }

    // Lista os convites que ainda estao pendentes.
    public List<ConviteResumoResposta> convitesPendentes() {
        return convites.findByStatusOrderByCriadoEmAsc(PENDENTE).stream()
                .map(convite -> new ConviteResumoResposta(convite.getId(), convite.getEmail(), convite.getPerfil(),
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
        usuarios.delete(usuario);
    }

    @Transactional
    // Remove uma pendencia de cadastro.
    public void remover(Long idUsuario, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        Usuario usuario = usuarioPendente(idUsuario);
        usuarios.deleteById(usuario.getId());
        auditoria.registrar(administradorId, "PENDENCIA_REMOVIDA", "Usuario " + idUsuario, null);
    }

    @Transactional
    // Reenvia um convite que ainda esta pendente.
    public void reenviar(Long idConvite, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        ConviteAcesso convite = convites.findById(idConvite)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        if (!PENDENTE.equals(convite.getStatus()))
            throw new IllegalArgumentException("Convite indisponivel");
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
        convites.delete(convite);
        auditoria.registrar(administradorId, "CONVITE_REMOVIDO", convite.getEmail(), null);
    }

    private Usuario usuarioPendente(Long id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pendencia nao encontrada"));
        if (usuario.getStatusAprovacao() != StatusAprovacao.PENDENTE_APROVACAO) {
            throw new IllegalArgumentException("Pendencia indisponivel");
        }
        return usuario;
    }

    private ConviteAcesso localizar(String token) {
        ConviteAcesso convite = convites.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
        if (!PENDENTE.equals(convite.getStatus()))
            throw new IllegalArgumentException("Convite indisponivel");
        return convite;
    }

    private boolean expirado(ConviteAcesso convite) {
        return convite.getExpiraEm().isBefore(LocalDateTime.now());
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
