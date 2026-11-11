package com.cursovalido.backend.forum.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AutoriaForumTest {
    @Test
    void topico_semRelacionamento_deveUsarDadosTransitoriosDoAutor() {
        var topico = new Topico();
        topico.setNomeAutor("Nome informado");
        topico.setPapelAutor("PROFESSOR");
        assertThat(topico.getNomeAutor()).isEqualTo("Nome informado");
        assertThat(topico.getPapelAutor()).isEqualTo("PROFESSOR");
    }

    @Test
    void topico_comRelacionamento_devePriorizarDadosAtuaisDoUsuario() {
        var topico = new Topico();
        topico.setNomeAutor("Nome antigo");
        topico.setPapelAutor("ALUNO");
        topico.setUsuarioAutor(new UsuarioForum(7L, "Nome atual", "autor@example.com", "PROFESSOR"));
        assertThat(topico.getNomeAutor()).isEqualTo("Nome atual");
        assertThat(topico.getPapelAutor()).isEqualTo("PROFESSOR");
    }

    @Test
    void comentario_semRelacionamento_deveUsarDadosTransitoriosDoAutor() {
        var comentario = new Comentario();
        comentario.setNomeAutor("Nome informado");
        comentario.setPapelAutor("PROFESSOR");
        assertThat(comentario.getNomeAutor()).isEqualTo("Nome informado");
        assertThat(comentario.getPapelAutor()).isEqualTo("PROFESSOR");
    }

    @Test
    void comentario_comRelacionamento_devePriorizarDadosAtuaisDoUsuario() {
        var comentario = new Comentario();
        comentario.setNomeAutor("Nome antigo");
        comentario.setPapelAutor("ALUNO");
        comentario.setUsuarioAutor(new UsuarioForum(7L, "Nome atual", "autor@example.com", "PROFESSOR"));
        assertThat(comentario.getNomeAutor()).isEqualTo("Nome atual");
        assertThat(comentario.getPapelAutor()).isEqualTo("PROFESSOR");
    }
}
