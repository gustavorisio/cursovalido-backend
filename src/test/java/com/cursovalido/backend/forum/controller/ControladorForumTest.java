package com.cursovalido.backend.forum.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.forum.dto.*;
import com.cursovalido.backend.forum.entity.*;
import com.cursovalido.backend.forum.service.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Conversao dos DTOs e delegacao; validacao HTTP e seguranca ficam na integracao real. */
@ExtendWith(MockitoExtension.class)
class ControladorForumTest {
    @Mock private TopicoServico topicos;
    @Mock private ComentarioServico comentarios;
    @Mock private UsuarioForumServico usuarios;
    @InjectMocks private ControladorForum controlador;
    private UsuarioAutenticado principal;
    private UsuarioForum autor;
    private Topico topico;
    private Comentario comentario;

    @BeforeEach
    void prepararDados() {
        principal = new UsuarioAutenticado(7L, "autor@example.com", "senha", "ALUNO", true, true, true);
        autor = new UsuarioForum(7L, "Autor", "autor@example.com", "ALUNO");
        topico = new Topico();
        topico.setId(10L);
        topico.setTitulo("Java");
        topico.setDescricao("Duvida");
        topico.setIdAutor(7L);
        topico.setUsuarioAutor(autor);
        topico.setCriadoEm(LocalDateTime.of(2026, 1, 1, 12, 0));
        comentario = new Comentario();
        comentario.setId(20L);
        comentario.setConteudo("Resposta");
        comentario.setIdAutor(7L);
        comentario.setUsuarioAutor(autor);
        comentario.setCriadoEm(topico.getCriadoEm());
    }

    @Test
    void listarTopicos_comParametros_deveEncaminharBuscaPaginaEUsuario() {
        var resumo = new TopicoResumoDTO(10L, "Java", "Duvida", "Autor", "ALUNO", 7L, topico.getCriadoEm(), 2, true, false);
        when(topicos.listarRecentes(1, 3, "java", 7L)).thenReturn(List.of(resumo));
        assertThat(controlador.listarTopicosRecentes(principal, 1, 3, "java")).containsExactly(resumo);
    }

    @Test
    void criarTopico_comRequisicao_deveConverterEntradaESaida() {
        when(topicos.criar(any(), eq(7L))).thenReturn(topico);
        var resposta = controlador.criarTopico(principal, new TopicoRequest("Java", "Duvida"));
        var captor = ArgumentCaptor.forClass(Topico.class);
        verify(topicos).criar(captor.capture(), eq(7L));
        assertThat(captor.getValue().getTitulo()).isEqualTo("Java");
        assertThat(captor.getValue().getDescricao()).isEqualTo("Duvida");
        assertThat(resposta).isEqualTo(new TopicoResponse(10L, "Java", "Duvida", "Autor", "ALUNO", 7L, topico.getCriadoEm(), 0, true, false));
    }

    @Test
    void editarTopico_comRequisicao_deveEncaminharIdEDados() {
        when(topicos.editar(eq(10L), any(), eq(7L))).thenReturn(topico);
        var resposta = controlador.editarTopico(principal, 10L, new TopicoRequest("Novo", "Descricao"));
        var captor = ArgumentCaptor.forClass(Topico.class);
        verify(topicos).editar(eq(10L), captor.capture(), eq(7L));
        assertThat(captor.getValue().getTitulo()).isEqualTo("Novo");
        assertThat(captor.getValue().getDescricao()).isEqualTo("Descricao");
        assertThat(resposta.id()).isEqualTo(10L);
    }

    @Test
    void arquivarTopico_comPrincipal_deveUsarUsuarioAutenticado() {
        controlador.arquivarTopico(principal, 10L);
        verify(topicos).arquivar(10L, 7L);
    }

    @Test
    void fecharTopico_comPrincipal_deveUsarUsuarioAutenticado() {
        controlador.fecharTopico(principal, 10L);
        verify(topicos).fechar(10L, 7L);
    }

    @Test
    void apagarTopico_comPrincipal_deveUsarUsuarioAutenticado() {
        controlador.apagarTopico(principal, 10L);
        verify(topicos).apagar(10L, 7L);
    }

    @Test
    void listarComentarios_comTopico_deveConverterRespostaSemExporEntidades() {
        when(comentarios.listarPorTopico(10L, 7L)).thenReturn(List.of(comentario));
        assertThat(controlador.listarComentarios(principal, 10L)).containsExactly(new ComentarioResponse(
                20L, "Resposta", "Autor", "ALUNO", 7L, comentario.getCriadoEm(), true));
    }

    @Test
    void adicionarComentario_comRequisicao_deveConverterEEncaminharRelacionamento() {
        when(comentarios.criar(eq(10L), any(), eq(7L))).thenReturn(comentario);
        var resposta = controlador.adicionarComentario(principal, 10L, new ComentarioRequest("Resposta"));
        var captor = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarios).criar(eq(10L), captor.capture(), eq(7L));
        assertThat(captor.getValue().getConteudo()).isEqualTo("Resposta");
        assertThat(resposta.id()).isEqualTo(20L);
    }

    @Test
    void editarComentario_comRequisicao_deveConverterEEncaminharId() {
        when(comentarios.editar(eq(20L), any(), eq(7L))).thenReturn(comentario);
        var resposta = controlador.editarComentario(principal, 20L, new ComentarioRequest("Atualizado"));
        var captor = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarios).editar(eq(20L), captor.capture(), eq(7L));
        assertThat(captor.getValue().getConteudo()).isEqualTo("Atualizado");
        assertThat(resposta.id()).isEqualTo(20L);
    }

    @Test
    void apagarComentario_comPrincipal_deveEncaminharIdEUsuario() {
        controlador.apagarComentario(principal, 20L);
        verify(comentarios).apagar(20L, 7L);
    }

    @Test
    void apagarComentarioDoTopico_comPrincipal_deveEncaminharRelacionamento() {
        controlador.apagarComentarioDoTopico(principal, 10L, 20L);
        verify(comentarios).apagarDoTopico(10L, 20L, 7L);
    }

    @Test
    void listarUsuarios_comUsuariosAtivos_deveConverterEContar() {
        when(usuarios.listarAtivos()).thenReturn(List.of(autor));
        assertThat(controlador.listarUsuarios()).isEqualTo(new RespostaUsuariosForum(
                List.of(new UsuarioForumResponse(7L, "Autor", "autor@example.com", "ALUNO")), 1));
    }
}
