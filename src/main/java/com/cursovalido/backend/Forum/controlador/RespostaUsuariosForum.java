package com.cursovalido.backend.Forum.controlador;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import java.util.List;

public class RespostaUsuariosForum {
    private final List<UsuarioForum> value;

    private final int count;

    public RespostaUsuariosForum(List<UsuarioForum> value) {
        this.value = value;
        this.count = value.size();
    }

    public List<UsuarioForum> getValue() {
        return value;
    }

    public int getCount() {
        return count;
    }
}