package com.cursovalido.backend.aulas.dto;

import com.cursovalido.backend.aulas.entity.Tema;

public record TemaResponse(Long id, String nome) {
    public static TemaResponse de(Tema tema) {
        return new TemaResponse(tema.getId(), tema.getNome());
    }
}
