package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class AdministradorAulasControllerTest {
    private final AdministradorAulasService servico = mock(AdministradorAulasService.class);
    private final AdministradorAulasController controlador = new AdministradorAulasController(servico);

    @Test
    void consultaAdministrativaIncluiDisponibilidadeDaAula() {
        var aula = aula(); aula.setAtivo(false);
        when(servico.buscarAula(30L, 7L)).thenReturn(aula);
        var resposta = controlador.buscarAula(30L, usuario());
        assertThat(resposta.ativo()).isFalse();
        assertThat(resposta.vimeoVideoUrl()).isEqualTo(aula.getVimeoVideoUrl());
        verify(servico).buscarAula(30L, 7L);
    }

    @Test
    void listaModulosDoCursoComAdministradorAutenticado() {
        when(servico.listarModulos(10L, 7L)).thenReturn(List.of(modulo()));
        assertThat(controlador.listarModulos(10L, usuario())).containsExactly(new ModuloResponse(20L, "Inicio", "Fundamentos", 10L, 2));
        verify(servico).listarModulos(10L, 7L);
    }

    @Test
    void edicaoMantemIdentidadeDoCursoEDoAdministrador() {
        var dados = new CursoRequest("Java", "Fundamentos");
        when(servico.editarCurso(10L, dados, 7L)).thenReturn(curso());
        assertThat(controlador.editarCurso(10L, dados, usuario()).idProfessor()).isEqualTo(7L);
        verify(servico).editarCurso(10L, dados, 7L);
    }
}
