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
            throw new IllegalArgumentException("Este código já foi usado. Peça um novo código para continuar.");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new Codigo2faExpiradoException();
        }
    }

    public static void validarToken(TokenEmail registro) {
        if (registro.isUtilizado())
            throw new IllegalArgumentException("Este link já foi usado. Solicite um novo link para continuar.");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Este link venceu. Solicite um novo link para continuar.");
    }

    public static void verificarLimite(Usuario usuario, String finalidade, TokenEmailRepository repositorio) {
        if (repositorio.countByUsuarioIdAndFinalidadeAndCriadoEmAfter(
                usuario.getId(), finalidade, LocalDateTime.now().minusMinutes(15)) >= 3)
            throw new LimiteReenvioException();
    }
}
