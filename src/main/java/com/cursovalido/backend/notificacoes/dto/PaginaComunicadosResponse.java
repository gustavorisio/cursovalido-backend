package com.cursovalido.backend.notificacoes.dto;

import java.util.List;

public record PaginaComunicadosResponse(List<ComunicadoResponse> itens, int pagina,
                int totalPaginas, long totalElementos, long naoLidas) {
}
