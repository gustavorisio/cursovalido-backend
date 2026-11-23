package com.cursovalido.backend.authentication.validation.token;

import com.cursovalido.backend.authentication.entity.Sessao;
import java.time.LocalDateTime;
import java.util.Objects;

public final class TokenJwtValidation {
    private TokenJwtValidation() {
    }

    public static void validarSessao(Sessao sessao, io.jsonwebtoken.Claims dados) {
        if (sessao.isRevogada())
            throw new IllegalArgumentException("Sessao encerrada");
        if (!sessao.getExpiraEm().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Sessao expirada");
        if (!Objects.equals(sessao.getUsuarioId(), dados.get("idUsuario", Long.class)))
            throw new IllegalArgumentException("Sessao de outro usuario");
    }
}
