package com.cursovalido.backend.forum.dto;

import java.util.List;

public record UsuariosForumResponse(List<UsuarioForumResponse> value, int count) {
    public UsuariosForumResponse(List<UsuarioForumResponse> value) {
        this(value, value.size());
    }
}