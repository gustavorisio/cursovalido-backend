package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.AutenticacaoResponse;
import com.cursovalido.backend.authentication.dto.LoginRequest;
import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.login.AutenticacaoValidation;
import com.cursovalido.backend.authentication.validation.login.ProtecaoForcaBrutaService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AutenticacaoService {
    private final UsuarioRepository repositorio;
    private final TokenJwtService tokens;
    private final TokenEmailService tokensEmail;
    private final EnvioCodigo2faService envio2fa;
    private final PasswordEncoder codificadorSenha;
    private final LogAuditoriaService auditoria;
    private final UsuarioForumService usuariosForum;
    private final ProtecaoForcaBrutaService protecaoForcaBruta;
    private final ModeracaoUsuarioService moderacao;
    private final PoliticaService politicas;
    private final ResponsabilidadeService responsabilidade;
    private final PrivacidadeService privacidade;
    private final PrivilegiosAdministrativosService privilegios;

    public AutenticacaoService(UsuarioRepository repositorio, TokenJwtService tokens, TokenEmailService tokensEmail,
            EnvioCodigo2faService envio2fa, PasswordEncoder codificadorSenha, LogAuditoriaService auditoria,
            UsuarioForumService usuariosForum, ProtecaoForcaBrutaService protecaoForcaBruta,
            ModeracaoUsuarioService moderacao, PoliticaService politicas,
            ResponsabilidadeService responsabilidade, PrivacidadeService privacidade) {
        this(repositorio, tokens, tokensEmail, envio2fa, codificadorSenha, auditoria, usuariosForum,
                protecaoForcaBruta, moderacao, politicas, responsabilidade, privacidade, null);
    }

    // Valida a senha e pede codigo quando o 2FA esta ativo ou o e-mail ainda nao foi confirmado.
    public AutenticacaoResponse autenticar(LoginRequest requisicao, String enderecoIp) {
        String identificador = identificador(requisicao.email());
        protecaoForcaBruta.verificar(identificador);
        Usuario usuario = repositorio.findByEmail(requisicao.email()).orElse(null);
        AutenticacaoValidation.validarCredenciais(usuario, requisicao.senha(), codificadorSenha, identificador,
                enderecoIp, protecaoForcaBruta, auditoria);
        AutenticacaoValidation.validarLogin(usuario, enderecoIp, auditoria, moderacao);
        if (usuario.getPerfil().equipe() || usuario.isDoisFatoresAtivos() || !usuario.isEmailConfirmado()) {
            envio2fa.enviar(usuario.getId(), enderecoIp, false);
            String finalidadeCodigo = usuario.getPerfil().equipe() || usuario.isDoisFatoresAtivos()
                    ? "SEGUNDO_FATOR" : "CONFIRMAR_EMAIL";
            return new AutenticacaoResponse(null, null, null, null, true, finalidadeCodigo);
        }
        protecaoForcaBruta.registrarSucesso(identificador);
        return concluirLogin(usuario, enderecoIp, false);
    }

    // Confirma o codigo 2FA e gera o token de acesso.
    public AutenticacaoResponse confirmarCodigo(String email, String codigo, String enderecoIp) {
        String identificador = identificador(email);
        protecaoForcaBruta.verificar(identificador);
        Usuario usuario;
        try {
            // Primeiro o codigo e conferido sem ser consumido; a situacao da conta e validada
            // antes de inutiliza-lo, para que uma recusa nao obrigue o envio de outro codigo.
            Long idUsuario = tokensEmail.conferirCodigo2fa(email, codigo);
            Usuario dono = repositorio.findById(idUsuario).orElse(null);
            AutenticacaoValidation.validarConfirmacao(dono, moderacao);
            usuario = tokensEmail.validarCodigo2fa(email, codigo);
        } catch (IllegalArgumentException | Codigo2faExpiradoException excecao) {
            protecaoForcaBruta.registrarFalha(identificador);
            Usuario informado = repositorio.findByEmail(email).orElse(null);
            auditoria.registrar(informado == null ? null : informado.getId(),
                    "CODIGO_2FA_RECUSADO", "Codigo invalido ou expirado", enderecoIp);
            throw excecao;
        }
        protecaoForcaBruta.registrarSucesso(identificador);
        usuario = repositorio.findById(usuario.getId()).orElse(null);
        AutenticacaoValidation.validarConfirmacao(usuario, moderacao);
        if (!usuario.isEmailConfirmado()) {
            usuario.setEmailConfirmado(true);
            repositorio.save(usuario);
        }
        return concluirLogin(usuario, enderecoIp, true);
    }

    private AutenticacaoResponse concluirLogin(Usuario usuario, String enderecoIp, boolean confirmouCodigo) {
        if (privilegios == null || !privilegios.ehPrincipal(usuario)) {
            AutenticacaoValidation.validarAceite(usuario, politicas, responsabilidade, privacidade, tokens);
        }
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(usuario.getId(), "LOGIN",
                confirmouCodigo ? "Login realizado após código de confirmação" : "Login realizado sem código 2FA",
                enderecoIp);
        return new AutenticacaoResponse(tokens.gerar(usuario), usuario.getId(), usuario.getNomeCompleto(),
                usuario.getPerfil(), false);
    }

    // Envia um novo codigo 2FA para o usuario ativo.
    public void reenviarCodigo2fa(String email, String enderecoIp) {
        repositorio.findByEmail(email)
                .filter(Usuario::isAtivo)
                .filter(usuario -> !moderacao.restrito(usuario.getId(), EscopoSuspensao.CONTA))
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.APROVADO)
                .filter(usuario -> usuario.getPerfil().equipe() || usuario.isDoisFatoresAtivos()
                        || !usuario.isEmailConfirmado())
                .ifPresent(usuario -> envio2fa.enviar(usuario.getId(), enderecoIp, true));
    }

    private String identificador(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
