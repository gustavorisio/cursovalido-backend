package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import java.time.LocalDateTime;

public record ConviteResumoResposta(Long id, String email, Perfil perfil, String status,
                boolean emailConfirmado, LocalDateTime criadoEm, LocalDateTime expiraEm) {
}
