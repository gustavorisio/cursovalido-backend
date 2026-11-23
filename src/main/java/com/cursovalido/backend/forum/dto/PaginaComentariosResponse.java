package com.cursovalido.backend.forum.dto;

import java.util.List;

public record PaginaComentariosResponse(List<ComentarioResponse> itens, int pagina, int totalPaginas,
                long totalElementos) {
}
