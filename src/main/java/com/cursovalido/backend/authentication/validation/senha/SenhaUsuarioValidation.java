package com.cursovalido.backend.authentication.validation.senha;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class SenhaUsuarioValidation {
    public static final String MENSAGEM_CONFIRMACAO = "A confirmação deve ser igual à nova senha.";

    private SenhaUsuarioValidation() {
    }

    public static void validarAlteracao(Usuario usuario, String senhaAtual, String novaSenha, String confirmacaoSenha,
            PasswordEncoder codificadorSenha) {
        if (!codificadorSenha.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        validarNovaSenha(novaSenha, confirmacaoSenha);
        if (codificadorSenha.matches(novaSenha, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }
    }

    public static void validarRecuperacao(String novaSenha, String confirmacaoSenha, LogAuditoriaService auditoria) {
        if (novaSenha == null || !novaSenha.equals(confirmacaoSenha)) {
            auditoria.registrar(null, "RECUPERACAO_FALHOU", "Senhas diferentes", null);
            throw new IllegalArgumentException(MENSAGEM_CONFIRMACAO);
        }
        RegrasSenha.primeiroProblema(novaSenha).ifPresent(problema -> {
            throw new IllegalArgumentException(problema);
        });
    }

    private static void validarNovaSenha(String novaSenha, String confirmacaoSenha) {
        RegrasSenha.primeiroProblema(novaSenha).ifPresent(problema -> {
            throw new IllegalArgumentException(problema);
        });
        if (!novaSenha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException(MENSAGEM_CONFIRMACAO);
        }
    }
}
