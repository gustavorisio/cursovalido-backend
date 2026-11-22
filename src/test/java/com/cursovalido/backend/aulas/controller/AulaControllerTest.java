package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class AulaControllerTest {
    private final AulaService servico = mock(AulaService.class);
    private final AulaController controlador = new AulaController(servico);

    @Test
    void listaAulasComOsDoisServidoresENaOrdemDoServico() {
        when(servico.listar(20L, 7L)).thenReturn(List.of(aula()));
        var resposta = controlador.listar(20L, usuario()).getFirst();
        assertThat(resposta.id()).isEqualTo(30L);
        assertThat(resposta.idModulo()).isEqualTo(20L);
        assertThat(resposta.ordem()).isEqualTo(2);
        assertThat(resposta.youtubeVideoId()).isEqualTo("abcdefghijk");
        assertThat(resposta.vimeoVideoUrl()).isEqualTo("https://vimeo.com/123");
        verify(servico).listar(20L, 7L);
    }

    @Test
    void consultaAulaUsandoIdentidadeAutenticadaEPreservaNegacaoDeAcesso() {
        when(servico.buscarAtivo(30L, 7L)).thenReturn(aula());
        assertThat(controlador.buscar(30L, usuario()).titulo()).isEqualTo("Primeira aula");
        var negado = new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        when(servico.buscarAtivo(99L, 7L)).thenThrow(negado);
        assertThatThrownBy(() -> controlador.buscar(99L, usuario())).isSameAs(negado);
    }
}
