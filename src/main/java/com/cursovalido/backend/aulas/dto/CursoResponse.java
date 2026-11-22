package com.cursovalido.backend.aulas.dto;

import com.cursovalido.backend.aulas.entity.Curso;

public record CursoResponse(Long id, String titulo, String descricao, Long idProfessor, Long idTema, String nomeTema,
        String capaJpeg) {
    public static CursoResponse de(Curso curso) {
        return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescricao(), curso.getIdProfessor(),
                curso.getTema() == null ? null : curso.getTema().getId(),
                curso.getTema() == null ? null : curso.getTema().getNome(), curso.getCapaJpeg());
    }
}
