package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusConvite;
import java.time.LocalDateTime;

public record ConviteResumoResponse(Long id, String email, Perfil perfil, StatusConvite status,
                boolean emailConfirmado, LocalDateTime criadoEm, LocalDateTime expiraEm) {
}
