package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;

public record CursoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 5000) String descricao,
        @Positive Long idTema,
        @Positive Long idArea,
        @Size(max = 2796227) String capaJpeg) {
    public CursoRequest(String titulo, String descricao, Long idTema, Long idArea) {
        this(titulo, descricao, idTema, idArea, null);
    }

    public CursoRequest(String titulo, String descricao) {
        this(titulo, descricao, null, null, null);
    }
}
