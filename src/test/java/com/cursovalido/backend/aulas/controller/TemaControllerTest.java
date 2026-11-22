package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class TemaControllerTest {
    @Test
    void listaTemasECriaUmNovoUsandoIdentidadeAutenticada() {
        var servico = mock(TemaService.class);
        var tema = new Tema(); tema.setId(5L); tema.setNome("Programacao");
        var controlador = new TemaController(servico);
        when(servico.listar()).thenReturn(List.of(tema));
        assertThat(controlador.listar()).extracting(TemaResponse::nome).containsExactly("Programacao");
        var dados = new TemaRequest("Programacao");
        when(servico.salvar(null, dados.nome(), 7L)).thenReturn(tema);
        assertThat(controlador.criar(dados, usuario()).id()).isEqualTo(5L);
        verify(servico).salvar(null, "Programacao", 7L);
        var edicao = new TemaRequest("Java");
        when(servico.salvar(5L, "Java", 7L)).thenReturn(tema);
        assertThat(controlador.editar(5L, edicao, usuario()).id()).isEqualTo(5L);
        verify(servico).salvar(5L, "Java", 7L);
    }
}
