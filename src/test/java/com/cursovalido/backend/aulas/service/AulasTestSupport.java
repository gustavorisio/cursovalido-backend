package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.authentication.entity.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

final class AulasTestSupport {
    private AulasTestSupport() { }

    static Curso curso() {
        Curso curso = new Curso();
        curso.setId(10L);
        curso.setIdProfessor(1L);
        curso.setTitulo("Java");
        curso.setDescricao("Basico");
        return curso;
    }

    static Modulo modulo() {
        Modulo modulo = new Modulo();
        modulo.setId(20L);
        modulo.setIdCurso(10L);
        modulo.setTitulo("Inicio");
        modulo.setDescricao("Basico");
        modulo.setOrdem(1);
        return modulo;
    }

    static Aula aula() {
        Aula aula = new Aula();
        aula.setId(30L);
        aula.setIdModulo(20L);
        aula.setTitulo("Java");
        aula.setDescricao("Basico");
        aula.setYoutubeVideoId("M7lc1UVf-VE");
        aula.setOrdem(1);
        return aula;
    }

    static Matricula matricula() {
        Matricula matricula = new Matricula();
        matricula.setId(40L);
        matricula.setIdAluno(1L);
        matricula.setIdCurso(10L);
        return matricula;
    }

    static Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setEmailConfirmado(true);
        usuario.setAceitouTermosLgpd(true);
        return usuario;
    }

    static void erro(Runnable acao, int status) {
        assertThatThrownBy(acao::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(status));
    }
}
