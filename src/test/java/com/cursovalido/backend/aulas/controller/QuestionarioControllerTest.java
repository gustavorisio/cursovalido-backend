package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class QuestionarioControllerTest {
    private final QuestionarioService servico = mock(QuestionarioService.class);
    private final QuestionarioController controlador = new QuestionarioController(servico);

    @Test
    void consultaPublicadaMantemCursoModuloEAlunoAutenticado() {
        var retorno = new QuestionarioAlunoResponse(3L, "Modulo", 2, List.of(), null);
        when(servico.consultarAluno(10L, 20L, 7L)).thenReturn(retorno);
        assertThat(controlador.consultarAluno(10L, 20L, usuario())).isSameAs(retorno);
    }

    @Test
    void respostasEncaminhamVersaoESemTrocarOAluno() {
        var dados = new RespostasQuestionarioRequest(2, List.of(1));
        var retorno = new ResultadoQuestionarioResponse(0, 1, 0, null, List.of(), List.of());
        when(servico.responder(10L, 20L, dados, 7L)).thenReturn(retorno);
        assertThat(controlador.responder(10L, 20L, dados, usuario())).isSameAs(retorno);
        verify(servico).responder(10L, 20L, dados, 7L);
    }

    @Test
    void consultaGestaoExigeModuloSelecionado() {
        var retorno = new QuestionarioGestaoResponse(3L, "Curso", true, 2, List.of());
        when(servico.consultarGestao(10L, 20L, 7L)).thenReturn(retorno);
        assertThat(controlador.consultarGestao(10L, 20L, usuario())).isSameAs(retorno);
    }
}
