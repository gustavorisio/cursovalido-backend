package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.cursovalido.backend.support.ControllerFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressoAulaControllerTest {
    @Test
    void conclusaoPreservaProgressoEDatasSemRecompensa() {
        var servico = mock(ProgressoAulaService.class);
        var progresso = new ProgressoAula();
        progresso.setIdAula(30L);
        progresso.setPosicaoSegundos(123);
        progresso.setConcluida(true);
        var data = LocalDateTime.of(2026, 11, 22, 10, 0);
        progresso.setConcluidaEm(data);
        progresso.setAtualizadoEm(data);
        when(servico.concluir(30L, 7L)).thenReturn(progresso);
        var resposta = new ProgressoAulaController(servico).concluir(30L, usuario());
        assertThat(resposta.concluida()).isTrue();
        assertThat(resposta.posicaoSegundos()).isEqualTo(123);
        assertThat(resposta.concluidaEm()).isEqualTo(data);
        assertThat(resposta.atualizadoEm()).isEqualTo(data);
        verify(servico).concluir(30L, 7L);
    }
}
