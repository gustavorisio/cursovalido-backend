package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.repository.RankingAlunoRepository;
import com.cursovalido.backend.aulas.dto.RankingAlunoResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingAlunoServiceTest {
    @Test
    void rankingAbreviaNomeSemAlterarOrdemOuPontuacao() {
        var repositorio = mock(RankingAlunoRepository.class);
        when(repositorio.listar()).thenReturn(List.of(new RankingAlunoResponse(1, "Ana Maria Silva", 3, 12, 8)));
        var resposta = new RankingAlunoService(repositorio).listar();
        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().nome()).isEqualTo("Ana S.");
        assertThat(resposta.getFirst().posicao()).isEqualTo(1);
        assertThat(resposta.getFirst().nivel()).isEqualTo(3);
        assertThat(resposta.getFirst().comentarios()).isEqualTo(12);
        assertThat(resposta.getFirst().aulasConcluidas()).isEqualTo(8);
    }
}
