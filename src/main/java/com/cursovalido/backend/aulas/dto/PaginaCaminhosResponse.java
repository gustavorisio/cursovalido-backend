package com.cursovalido.backend.aulas.dto;

import java.util.List;

public record PaginaCaminhosResponse(List<Resumo> questionarios, List<SecaoCaminhosResponse> secoes) {
    public record Resumo(Long id, String titulo, String descricao, boolean publicado, int versao) {}
}
