package com.cursovalido.backend.forum.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record UsuarioForumResponse(Long id, String nome,
                @JsonInclude(JsonInclude.Include.NON_NULL) String email, String perfil) {
}
