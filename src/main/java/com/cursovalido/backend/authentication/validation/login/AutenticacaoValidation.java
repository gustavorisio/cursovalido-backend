package com.cursovalido.backend.authentication.validation.login;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.AcessoRejeitadoException;
import com.cursovalido.backend.authentication.exception.BanimentoException;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.exception.EmailNaoConfirmadoException;
import com.cursovalido.backend.authentication.exception.PendenteAprovacaoException;
import com.cursovalido.backend.authentication.exception.TermosNaoAceitosException;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioService;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.PrivacidadeService;
import com.cursovalido.backend.authentication.service.ResponsabilidadeService;
import com.cursovalido.backend.authentication.service.TokenJwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class AutenticacaoValidation {
    private AutenticacaoValidation() {
    }

    public static void validarLogin(Usuario usuario, String enderecoIp, LogAuditoriaService auditoria,
            ModeracaoUsuarioService moderacao) {
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
        verificarSuspensao(usuario, moderacao);
    }

    public static void validarConfirmacao(Usuario usuario, ModeracaoUsuarioService moderacao, PoliticaService politicas,
            ResponsabilidadeService responsabilidade, PrivacidadeService privacidade, TokenJwtService tokens) {
        verificarSuspensao(usuario, moderacao);
        if (!usuario.isAtivo())
            throw new CredenciaisInvalidasException();
        if (!usuario.isEmailConfirmado())
            throw new CredenciaisInvalidasException();
        if (usuario.getStatusAprovacao() != StatusAprovacao.APROVADO)
            throw new CredenciaisInvalidasException();
        if (!aceiteAtualizado(usuario, politicas, responsabilidade, privacidade))
            throw new TermosNaoAceitosException(tokens.gerarTermos(usuario));
    }

    private static boolean aceiteAtualizado(Usuario usuario, PoliticaService politicas,
            ResponsabilidadeService responsabilidade, PrivacidadeService privacidade) {
        if (!Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())) {
            return false;
        }
        if (!politicas.versaoAtiva(usuario.getVersaoTermos())) {
            return false;
        }
        return !privacidade.atualizadaDesde(usuario.getDataHoraAceite()) && responsabilidade.atualizado(usuario);
    }

    private static boolean senhaValida(Usuario usuario, String senha, PasswordEncoder codificadorSenha) {
        if (usuario == null) {
            return false;
        }
        return codificadorSenha.matches(senha, usuario.getSenha());
    }

    private static void verificarSuspensao(Usuario usuario, ModeracaoUsuarioService moderacao) {
        if (moderacao.restrito(usuario.getId(), EscopoSuspensao.CONTA)) {
            throw new BanimentoException(
                    moderacao.mensagemBanimento(usuario.getId(), EscopoSuspensao.CONTA));
        }
    }

    public static void validarCredenciais(Usuario usuario, String senha, PasswordEncoder codificadorSenha,
            String identificador, String enderecoIp, ProtecaoForcaBrutaService protecaoForcaBruta,
            LogAuditoriaService auditoria) {
        if (!senhaValida(usuario, senha, codificadorSenha)) {
            protecaoForcaBruta.registrarFalha(identificador);
            auditoria.registrar(usuario == null ? null : usuario.getId(),
                    "LOGIN_RECUSADO", "Credenciais invalidas", enderecoIp);
            throw new CredenciaisInvalidasException();
        }
    }
}
