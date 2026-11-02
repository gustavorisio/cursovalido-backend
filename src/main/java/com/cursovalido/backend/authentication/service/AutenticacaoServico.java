package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.AutenticacaoResposta;
import com.cursovalido.backend.authentication.dto.LoginRequisicao;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.AcessoRejeitadoException;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.EmailNaoConfirmadoException;
import com.cursovalido.backend.authentication.exception.PendenteAprovacaoException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacaoServico {
    private final UsuarioRepositorio repositorio;
    private final TokenJwtServico tokens;
    private final TokenEmailServico tokensEmail;
    private final EmailServico emails;
    private final PasswordEncoder codificadorSenha;
    private final LogAuditoriaServico auditoria;
    private final UsuarioForumServico usuariosForum;
    private final ProtecaoForcaBrutaServico protecaoForcaBruta;

    // Valida a senha e inicia o login com o envio do codigo 2FA.
    public AutenticacaoResposta autenticar(LoginRequisicao requisicao, String enderecoIp) {
        String identificador = identificador(requisicao.email(), enderecoIp);
        protecaoForcaBruta.verificar(identificador);
        Usuario usuario = repositorio.findByEmail(requisicao.email()).orElse(null);
        if (!senhaValida(usuario, requisicao.senha())) {
            protecaoForcaBruta.registrarFalha(identificador);
            auditoria.registrar(usuario == null ? null : usuario.getId(),
                    "LOGIN_RECUSADO", "Credenciais invalidas", enderecoIp);
            throw new CredenciaisInvalidasException();
        }
        if (usuario.getStatusAprovacao() == StatusAprovacao.PENDENTE_APROVACAO) {
            auditoria.registrar(usuario.getId(), "LOGIN_RECUSADO", "Aprovacao pendente", enderecoIp);
            throw new PendenteAprovacaoException();
        }
        if (usuario.getStatusAprovacao() == StatusAprovacao.REJEITADO) {
            auditoria.registrar(usuario.getId(), "LOGIN_RECUSADO", "Acesso rejeitado", enderecoIp);
            throw new AcessoRejeitadoException();
        }
        if (!usuario.isEmailConfirmado())
            throw new EmailNaoConfirmadoException();
        if (!usuario.isAtivo())
            throw new CredenciaisInvalidasException();
        if (!Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())) {
            auditoria.registrar(usuario.getId(), "LOGIN_RECUSADO",
                    "Termos precisam ser aceitos novamente", enderecoIp);
            throw new TermosNaoAceitosException(tokens.gerarTermos(usuario));
        }
        protecaoForcaBruta.registrarSucesso(identificador);
        String codigo = tokensEmail.criarCodigo2fa(usuario);
        emails.enviarCodigo2fa(usuario, codigo);
        auditoria.registrar(usuario.getId(), "CODIGO_2FA_ENVIADO", "Codigo enviado por e-mail", enderecoIp);
        return new AutenticacaoResposta(null, null, null, null, true);
    }

    // Confirma o codigo 2FA e gera o token de acesso.
    public AutenticacaoResposta confirmarCodigo(String email, String codigo, String enderecoIp) {
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
        if (!usuario.isAtivo() || !usuario.isEmailConfirmado()
                || usuario.getStatusAprovacao() != StatusAprovacao.APROVADO
                || !Boolean.TRUE.equals(usuario.getAceitouTermosLgpd()))
            throw new CredenciaisInvalidasException();
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(usuario.getId(), "LOGIN", "Login realizado com 2FA", enderecoIp);
        return new AutenticacaoResposta(tokens.gerar(usuario), usuario.getId(), usuario.getNomeCompleto(),
                usuario.getPerfil(), false);
    }

    // Envia um novo codigo 2FA para o usuario ativo.
    public void reenviarCodigo2fa(String email, String enderecoIp) {
        repositorio.findByEmail(email)
                .filter(Usuario::isAtivo)
                .filter(Usuario::isEmailConfirmado)
                .filter(usuario -> usuario.getStatusAprovacao() == StatusAprovacao.APROVADO)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAceitouTermosLgpd()))
                .filter(tokensEmail::temCodigo2faPendente)
                .ifPresent(usuario -> {
                    String codigo = tokensEmail.criarCodigo2fa(usuario);
                    emails.enviarCodigo2fa(usuario, codigo);
                    auditoria.registrar(usuario.getId(), "CODIGO_2FA_ENVIADO",
                            "Codigo 2FA reenviado por e-mail", enderecoIp);
                });
    }

    private boolean senhaValida(Usuario usuario, String senha) {
        return usuario != null && codificadorSenha.matches(senha, usuario.getSenha());
    }

    private String identificador(String email, String enderecoIp) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
