package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;

public record ConviteDadosResponse(String email, Perfil perfil) {
}
