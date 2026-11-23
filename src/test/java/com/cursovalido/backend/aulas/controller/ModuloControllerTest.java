package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class ModuloControllerTest {
    private final ModuloService servico = mock(ModuloService.class);
    private final ModuloController controlador = new ModuloController(servico);

    @Test
    void listaModulosDoCursoPreservandoIdentidadeEOrdem() {
        when(servico.listar(10L, 7L)).thenReturn(List.of(modulo()));
        assertThat(controlador.listar(10L, usuario()))
                .containsExactly(new ModuloResponse(20L, "Inicio", "Fundamentos", 10L, 2));
        verify(servico).listar(10L, 7L);
    }

    @Test
    void cadastroEncaminhaCursoEProfessorSemTrocarOsDados() {
        var dados = new ModuloRequest("Inicio", "Fundamentos", 2);
        when(servico.criar(10L, dados, 7L)).thenReturn(modulo());
        assertThat(controlador.criar(10L, usuario(), dados).idCurso()).isEqualTo(10L);
        verify(servico).criar(10L, dados, 7L);
    }
}
