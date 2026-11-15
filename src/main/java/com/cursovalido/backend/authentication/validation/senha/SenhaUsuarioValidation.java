package com.cursovalido.backend.authentication.validation.senha;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class SenhaUsuarioValidation {
    private SenhaUsuarioValidation() {
    }

    public static void validarAlteracao(Usuario usuario, String senhaAtual, String novaSenha, String confirmacaoSenha,
            PasswordEncoder codificadorSenha) {
        if (!codificadorSenha.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (!novaSenha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("As senhas nao conferem.");
        }
        if (!new SenhaValidaValidator().isValid(novaSenha, null)) {
            throw new IllegalArgumentException(SenhaValida.MENSAGEM);
        }
    }

    public static void validarRecuperacao(String novaSenha, String confirmacaoSenha, LogAuditoriaService auditoria) {
        if (!novaSenha.equals(confirmacaoSenha)) {
            auditoria.registrar(null, "RECUPERACAO_FALHOU", "Senhas diferentes", null);
            throw new IllegalArgumentException("As senhas nao conferem");
        }
    }
}
