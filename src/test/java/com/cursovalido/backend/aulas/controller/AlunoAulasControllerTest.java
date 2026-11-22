package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class AlunoAulasControllerTest {
    private final AlunoAulasService servico = mock(AlunoAulasService.class);
    private final AlunoAulasController controlador = new AlunoAulasController(servico);

    @Test
    void listaCursosAutorizadosDoAluno() {
        when(servico.listarCursos(7L)).thenReturn(List.of(curso()));
        assertThat(controlador.listarCursos(usuario()).getFirst().id()).isEqualTo(10L);
        verify(servico).listarCursos(7L);
    }

    @Test
    void matriculaUsaAlunoAutenticadoEDevolveDataPersistida() {
        var matricula = new Matricula(); matricula.setId(40L); matricula.setIdCurso(10L);
        var data = java.time.LocalDateTime.of(2026, 11, 22, 10, 0); matricula.setMatriculadoEm(data);
        when(servico.matricular(10L, 7L)).thenReturn(matricula);
        assertThat(controlador.matricular(10L, usuario())).isEqualTo(new MatriculaResponse(40L, 10L, data));
        verify(servico).matricular(10L, 7L);
    }

    @Test
    void aulaPreservaLinksEDescricaoParaReproducao() {
        when(servico.buscarAula(30L, 7L)).thenReturn(aula());
        var resposta = controlador.buscarAula(30L, usuario());
        assertThat(resposta.idModulo()).isEqualTo(20L);
        assertThat(resposta.vimeoVideoUrl()).isEqualTo("https://vimeo.com/123");
        verify(servico).buscarAula(30L, 7L);
    }
}
