package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.Matricula;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SincronizacaoGamificacaoTest {
    private final GamificacaoService gamificacao = mock(GamificacaoService.class);
    private final SincronizacaoGamificacao sincronizacao = new SincronizacaoGamificacao(gamificacao);

    @Test
    void registraConclusoesPendentesEContinuaAposFalhaPontual() {
        Matricula primeira = AulasTestSupport.matricula();
        Matricula segunda = AulasTestSupport.matricula();
        segunda.setId(41L);
        when(gamificacao.matriculasPendentesDeConclusao()).thenReturn(List.of(primeira, segunda));
        when(gamificacao.registrarConclusao(1L, 10L, 40L)).thenThrow(new IllegalStateException("falha"));
        when(gamificacao.registrarConclusao(1L, 10L, 41L))
                .thenReturn(true);

        assertThatCode(sincronizacao::sincronizarConclusoesAnteriores).doesNotThrowAnyException();
        verify(gamificacao).registrarConclusao(1L, 10L, 41L);
    }

    @Test
    void falhaAoListarPendentesNaoImpedeInicializacao() {
        when(gamificacao.matriculasPendentesDeConclusao()).thenThrow(new IllegalStateException("banco"));

        assertThatCode(sincronizacao::sincronizarConclusoesAnteriores).doesNotThrowAnyException();
        verify(gamificacao, never()).registrarConclusao(any(), any(), any());
    }
}
