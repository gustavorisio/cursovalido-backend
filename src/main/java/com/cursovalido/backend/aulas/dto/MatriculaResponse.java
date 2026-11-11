package com.cursovalido.backend.aulas.dto;

import java.time.LocalDateTime;

public record MatriculaResponse(Long id, Long idCurso, LocalDateTime matriculadoEm) {
}
