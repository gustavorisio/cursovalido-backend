package com.cursovalido.backend.forum.dto;

import java.util.List;

public record RespostaUsuariosForum(List<UsuarioForumResponse> value, int count) {
    public RespostaUsuariosForum(List<UsuarioForumResponse> value) {
        this(value, value.size());
    }
}