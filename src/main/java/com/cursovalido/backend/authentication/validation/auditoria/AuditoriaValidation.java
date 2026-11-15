package com.cursovalido.backend.authentication.validation.auditoria;

import java.nio.charset.StandardCharsets;

public final class AuditoriaValidation {
    private AuditoriaValidation() {
    }

    public static void validarChave(String chaveTexto) {
        if (chaveTexto == null)
            throw new IllegalStateException("AUDITORIA_CHAVE deve ter pelo menos 32 bytes");
        if (chaveTexto.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("AUDITORIA_CHAVE deve ter pelo menos 32 bytes");
    }
}
