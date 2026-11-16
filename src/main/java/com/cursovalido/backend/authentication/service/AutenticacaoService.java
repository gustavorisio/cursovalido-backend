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
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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

    // Valida a senha e inicia o login com o envio do codigo 2FA.
    public AutenticacaoResponse autenticar(LoginRequest requisicao, String enderecoIp) {
        String identificador = identificador(requisicao.email(), enderecoIp);
        protecaoForcaBruta.verificar(identificador);
        Usuario usuario = repositorio.findByEmail(requisicao.email()).orElse(null);
        AutenticacaoValidation.validarCredenciais(usuario, requisicao.senha(), codificadorSenha, identificador,
                enderecoIp, protecaoForcaBruta, auditoria);
        AutenticacaoValidation.validarLogin(usuario, enderecoIp, auditoria, moderacao);
        protecaoForcaBruta.registrarSucesso(identificador);
        envio2fa.enviar(usuario.getId(), enderecoIp, false);
        return new AutenticacaoResponse(null, null, null, null, true);
    }

    // Confirma o codigo 2FA e gera o token de acesso.
    public AutenticacaoResponse confirmarCodigo(String email, String codigo, String enderecoIp) {
        String identificador = identificador(email, enderecoIp) + ":2fa";
        protecaoForcaBruta.verificar(identificador);
        Usuario usuario;
        try {
            usuario = tokensEmail.validarCodigo2fa(email, codigo);
        } catch (IllegalArgumentException | Codigo2faExpiradoException excecao) {
            protecaoForcaBruta.registrarFalha(identificador);
            Usuario informado = repositorio.findByEmail(email).orElse(null);
            auditoria.registrar(informado == null ? null : informado.getId(),
                    "CODIGO_2FA_RECUSADO", "Codigo invalido ou expirado", enderecoIp);
            throw excecao;
        }
        protecaoForcaBruta.registrarSucesso(identificador);
        AutenticacaoValidation.validarConfirmacao(usuario, moderacao);
        if (!usuario.isEmailConfirmado()) {
            usuario.setEmailConfirmado(true);
            repositorio.save(usuario);
        }
        AutenticacaoValidation.validarAceite(usuario, politicas, responsabilidade, privacidade, tokens);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(usuario.getId(), "LOGIN", "Login realizado com 2FA", enderecoIp);
        return new AutenticacaoResponse(tokens.gerar(usuario), usuario.getId(), usuario.getNomeCompleto(),
                usuario.getPerfil(), false);
    }

    // Envia um novo codigo 2FA para o usuario ativo.
    public void reenviarCodigo2fa(String email, String enderecoIp) {
        repositorio.findByEmail(email)
                .filter(Usuario::isAtivo)
                .filter(usuario -> !moderacao.restrito(usuario.getId(), EscopoSuspensao.CONTA))
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.APROVADO)
                .ifPresent(usuario -> envio2fa.enviar(usuario.getId(), enderecoIp, true));
    }

    private String identificador(String email, String enderecoIp) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
