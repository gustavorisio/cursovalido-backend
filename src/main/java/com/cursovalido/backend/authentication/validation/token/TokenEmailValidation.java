package com.cursovalido.backend.authentication.validation.token;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import com.cursovalido.backend.authentication.exception.LimiteReenvioException;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import java.time.LocalDateTime;

public final class TokenEmailValidation {
    private TokenEmailValidation() {
    }

    public static void validarCodigo2fa(TokenEmail registro) {
        if (registro.isUtilizado())
            throw new IllegalArgumentException("Codigo ja utilizado");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new Codigo2faExpiradoException();
        }
    }

    public static void validarToken(TokenEmail registro) {
        if (registro.isUtilizado())
            throw new IllegalArgumentException("Token ja utilizado");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Token expirado");
    }

    public static void verificarLimite(Usuario usuario, String finalidade, TokenEmailRepository repositorio) {
        if (repositorio.countByUsuarioIdAndFinalidadeAndCriadoEmAfter(
                usuario.getId(), finalidade, LocalDateTime.now().minusMinutes(15)) >= 3)
            throw new LimiteReenvioException();
    }
}
