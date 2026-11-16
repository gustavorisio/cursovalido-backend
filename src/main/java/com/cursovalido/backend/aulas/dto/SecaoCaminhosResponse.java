package com.cursovalido.backend.aulas.dto;

import com.cursovalido.backend.aulas.entity.SecaoCaminhos;

public record SecaoCaminhosResponse(Long id, String titulo, String conteudo, int ordem, boolean publicado) {
    public static SecaoCaminhosResponse de(SecaoCaminhos secao) {
        return new SecaoCaminhosResponse(secao.getId(), secao.getTitulo(), secao.getConteudo(),
                secao.getOrdem(), secao.isPublicado());
    }
}
