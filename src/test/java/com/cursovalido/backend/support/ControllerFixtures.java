package com.cursovalido.backend.support;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import static org.mockito.Mockito.*;

public final class ControllerFixtures {
    private ControllerFixtures() {
    }

    public static UsuarioAutenticado usuario() {
        var usuario = mock(UsuarioAutenticado.class);
        when(usuario.getId()).thenReturn(7L);
        return usuario;
    }

    public static Curso curso() {
        var curso = new Curso();
        curso.setId(10L);
        curso.setIdProfessor(7L);
        curso.setTitulo("Java");
        curso.setDescricao("Fundamentos");
        return curso;
    }

    public static Aula aula() {
        var aula = new Aula();
        aula.setId(30L);
        aula.setIdModulo(20L);
        aula.setTitulo("Primeira aula");
        aula.setDescricao("Introducao");
        aula.setOrdem(2);
        aula.setYoutubeVideoId("abcdefghijk");
        aula.setVimeoVideoUrl("https://vimeo.com/123");
        return aula;
    }

    public static Modulo modulo() {
        var modulo = new Modulo();
        modulo.setId(20L);
        modulo.setIdCurso(10L);
        modulo.setTitulo("Inicio");
        modulo.setDescricao("Fundamentos");
        modulo.setOrdem(2);
        return modulo;
    }
}
