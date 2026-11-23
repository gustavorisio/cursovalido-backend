package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class CursoControllerTest {
    private final CursoService servico = mock(CursoService.class);
    private final CursoController controlador = new CursoController(servico);

    @Test
    void catalogoPublicoMantemTemaECapaSemExigirIdentidade() {
        var curso = curso();
        var tema = new Tema();
        tema.setId(5L);
        tema.setNome("Programacao");
        curso.setTema(tema);
        curso.setCapaJpeg("data:image/jpeg;base64,imagem");
        when(servico.listarPublicos()).thenReturn(List.of(curso));
        var item = controlador.catalogoPublico().getFirst();
        assertThat(item.id()).isEqualTo(10L);
        assertThat(item.idTema()).isEqualTo(5L);
        assertThat(item.nomeTema()).isEqualTo("Programacao");
        assertThat(item.capaJpeg()).isEqualTo(curso.getCapaJpeg());
        curso.setTema(null);
        assertThat(controlador.catalogoPublico().getFirst().idTema()).isNull();
        assertThat(controlador.catalogoPublico().getFirst().nomeTema()).isNull();
    }

    @Test
    void professorSemCursosRecebeListaVaziaDoSeuUsuario() {
        when(servico.listar(7L)).thenReturn(List.of());
        assertThat(controlador.listar(usuario())).isEmpty();
        verify(servico).listar(7L);
    }
}
