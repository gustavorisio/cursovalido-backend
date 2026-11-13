package com.cursovalido.backend.aulas.dto;

import com.cursovalido.backend.aulas.entity.SecaoCaminhos;
import java.util.List;

public record PaginaCaminhosResponse(List<Resumo> questionarios, List<SecaoCaminhos> secoes) {
    public record Resumo(Long id, String titulo, String descricao, boolean publicado, int versao) {}
}
