package com.cursovalido.backend.aulas.dto;

import com.cursovalido.backend.aulas.entity.Curso;

public record CursoResponse(Long id, String titulo, String descricao, Long idProfessor, Long idTema, String nomeTema, Long idArea, String nomeArea) {
    public static CursoResponse de(Curso curso) {
        return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescricao(), curso.getIdProfessor(),
                curso.getTema() == null ? null : curso.getTema().getId(),
                curso.getTema() == null ? null : curso.getTema().getNome(), curso.getArea() == null ? null : curso.getArea().getId(), curso.getArea() == null ? null : curso.getArea().getNome());
    }
}
