package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;

public record TermoResponsabilidadeResponse(Perfil perfil, String versao, String conteudo) {
}
