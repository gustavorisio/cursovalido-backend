package com.cursovalido.backend.authentication.validation.conta;

import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;

public final class ContaValidation {
    private ContaValidation() {
    }

    public static void validarPendencia(Usuario usuario) {
        if (usuario
                .getStatusAprovacao() != StatusAprovacao.PENDENTE_APROVACAO
                || usuario.getExcluidoEm() != null)
            throw new IllegalArgumentException("Este cadastro não está mais aguardando aprovação.");
    }

    public static void validarExclusao(Usuario usuario) {
        if (usuario.getExcluidoEm() != null)
            throw new IllegalArgumentException("Esta conta já foi excluída.");
    }
}
