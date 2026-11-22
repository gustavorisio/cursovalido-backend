package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class ProgressoAulaControllerTest {
    @Test
    void conclusaoPreservaProgressoRecompensaEDatasPersistidas() {
        var servico = mock(ProgressoAulaService.class);
        var progresso = new ProgressoAula(); progresso.setIdAula(30L); progresso.setPosicaoSegundos(123); progresso.setConcluida(true);
        var data = java.time.LocalDateTime.of(2026, 11, 22, 10, 0);
        progresso.setConcluidaEm(data); progresso.setAtualizadoEm(data);
        var recompensa = new RecompensaCursoResponse(10L, 50, 1, 2, true, null);
        when(servico.concluirComRecompensa(30L, 7L)).thenReturn(new ProgressoAulaService.ResultadoConclusao(progresso, recompensa));
        var resposta = new ProgressoAulaController(servico).concluir(30L, usuario());
        assertThat(resposta.concluida()).isTrue();
        assertThat(resposta.posicaoSegundos()).isEqualTo(123);
        assertThat(resposta.concluidaEm()).isEqualTo(data);
        assertThat(resposta.atualizadoEm()).isEqualTo(data);
        assertThat(resposta.recompensa()).isSameAs(recompensa);
        verify(servico).concluirComRecompensa(30L, 7L);
    }
}
