package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;

public record ConviteAcessoResponse(Long id, String email, Perfil perfil, StatusAprovacao status,
        String nomeCompleto) {
}
