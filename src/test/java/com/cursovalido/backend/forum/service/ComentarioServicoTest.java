package com.cursovalido.backend.forum.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.forum.entity.*;
import com.cursovalido.backend.forum.exception.*;
import com.cursovalido.backend.forum.repository.ComentarioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComentarioServicoTest {
    @Mock private ComentarioRepositorio repositorio;
    @Mock private TopicoServico topicos;
    @Mock private UsuarioForumServico usuarios;
    @Mock private LogAuditoriaService auditoria;
    @InjectMocks private ComentarioServico servico;
    private UsuarioForum autor;
    private Topico topico;
    private Comentario comentario;

    @BeforeEach
    void prepararDados() {
        autor = new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO");
        topico = new Topico();
        topico.setId(10L);
        comentario = new Comentario();
        comentario.setId(20L);
        comentario.setIdAutor(7L);
        comentario.setTopico(topico);
        comentario.setConteudo("Original");
    }

    @Test
    void listarPorTopico_comTopicoAtivo_deveListarERegistrarConsulta() {
        when(repositorio.listarPorTopicoAtivo(10L)).thenReturn(List.of(comentario));
        assertThat(servico.listarPorTopico(10L, 7L)).containsExactly(comentario);
        verify(topicos).buscarAtivo(10L);
        verify(auditoria).registrar(7L, "CONSULTA_COMENTARIOS", "Topico 10", null);
    }

    @Test
    void listarPorTopico_semUsuario_deveDelegarConsulta() {
        when(repositorio.listarPorTopicoAtivo(10L)).thenReturn(List.of());
        assertThat(servico.listarPorTopico(10L)).isEmpty();
        verify(auditoria).registrar(null, "CONSULTA_COMENTARIOS", "Topico 10", null);
    }

    @Test
    void listarPorTopico_comTopicoInexistente_deveInterromperConsulta() {
        when(topicos.buscarAtivo(10L)).thenThrow(RecursoNaoEncontradoException.topicoNaoEncontrado());
        assertThatThrownBy(() -> servico.listarPorTopico(10L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(repositorio, auditoria);
    }

    @Test
    void criar_comTopicoAberto_deveDefinirAutorDataERelacionamento() {
        when(usuarios.buscarAtivo(7L)).thenReturn(autor);
        when(topicos.buscarAtivo(10L)).thenReturn(topico);
        when(repositorio.save(comentario)).thenReturn(comentario);
        var antes = LocalDateTime.now();
        assertThat(servico.criar(10L, comentario, 7L)).isSameAs(comentario);
        assertThat(comentario.getUsuarioAutor()).isSameAs(autor);
        assertThat(comentario.getIdAutor()).isEqualTo(7L);
        assertThat(comentario.getTopico()).isSameAs(topico);
        assertThat(comentario.getCriadoEm()).isBetween(antes, LocalDateTime.now());
        assertThat(comentario.isAtivo()).isTrue();
        verify(auditoria).registrar(7L, "CADASTRO_COMENTARIO", "Comentario 20", null);
    }

    @Test
    void criar_comTopicoFechado_deveRejeitarSemPersistir() {
        topico.setFechado(true);
        when(usuarios.buscarAtivo(7L)).thenReturn(autor);
        when(topicos.buscarAtivo(10L)).thenReturn(topico);
        assertThatThrownBy(() -> servico.criar(10L, comentario, 7L)).isInstanceOf(ConflitoException.class);
        verifyNoInteractions(repositorio, auditoria);
    }

    @Test
    void criar_comUsuarioInativo_deveInterromperOperacao() {
        when(usuarios.buscarAtivo(7L)).thenThrow(RecursoNaoEncontradoException.usuarioNaoEncontrado());
        assertThatThrownBy(() -> servico.criar(10L, comentario, 7L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(topicos, repositorio, auditoria);
    }

    @Test
    void editar_comAutor_deveAtualizarSomenteConteudo() {
        prepararEdicao();
        Comentario dados = new Comentario();
        dados.setConteudo("Atualizado");
        when(repositorio.save(comentario)).thenReturn(comentario);
        assertThat(servico.editar(20L, dados, 7L).getConteudo()).isEqualTo("Atualizado");
        assertThat(comentario.getIdAutor()).isEqualTo(7L);
        assertThat(comentario.getTopico()).isSameAs(topico);
        verify(auditoria).registrar(7L, "EDICAO_COMENTARIO", "Comentario 20", null);
    }

    @Test
    void editar_comOutroUsuario_deveNegarAlteracao() {
        comentario.setIdAutor(99L);
        prepararEdicao();
        assertThatThrownBy(() -> servico.editar(20L, new Comentario(), 7L)).isInstanceOf(AcessoNegadoException.class);
        verify(repositorio, never()).save(any());
        assertThat(comentario.getConteudo()).isEqualTo("Original");
    }

    @Test
    void editar_comComentarioApagado_deveRejeitar() {
        comentario.setAtivo(false);
        prepararEdicao();
        assertThatThrownBy(() -> servico.editar(20L, new Comentario(), 7L)).isInstanceOf(RequisicaoInvalidaException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void editar_comIdInexistente_deveRetornarNaoEncontrado() {
        when(usuarios.buscarAtivo(7L)).thenReturn(autor);
        when(repositorio.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.editar(20L, new Comentario(), 7L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void apagar_comAutor_deveDesativarERegistrarAuditoria() {
        prepararEdicao();
        servico.apagar(20L, 7L);
        assertThat(comentario.isAtivo()).isFalse();
        verify(repositorio).save(comentario);
        verify(auditoria).registrar(7L, "EXCLUSAO_COMENTARIO", "Comentario 20", null);
    }

    @Test
    void apagar_comAdministrador_devePermitirExcluirDeOutroAutor() {
        comentario.setIdAutor(99L);
        prepararEdicao();
        when(usuarios.ehAdministrador(autor)).thenReturn(true);
        servico.apagar(20L, 7L);
        assertThat(comentario.isAtivo()).isFalse();
        verify(repositorio).save(comentario);
    }

    @Test
    void apagar_comOutroUsuario_deveNegarSemPersistir() {
        comentario.setIdAutor(99L);
        prepararEdicao();
        assertThatThrownBy(() -> servico.apagar(20L, 7L)).isInstanceOf(AcessoNegadoException.class);
        assertThat(comentario.isAtivo()).isTrue();
        verify(repositorio, never()).save(any());
    }

    @Test
    void apagarDoTopico_comRelacionamentoCorreto_deveDesativarComentario() {
        prepararEdicao();
        servico.apagarDoTopico(10L, 20L, 7L);
        assertThat(comentario.isAtivo()).isFalse();
        verify(repositorio).save(comentario);
    }

    @Test
    void apagarDoTopico_comOutroTopico_deveRetornarNaoEncontrado() {
        when(repositorio.findById(20L)).thenReturn(Optional.of(comentario));
        assertThatThrownBy(() -> servico.apagarDoTopico(99L, 20L, 7L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void apagarDoTopico_semTopico_deveRetornarNaoEncontrado() {
        comentario.setTopico(null);
        when(repositorio.findById(20L)).thenReturn(Optional.of(comentario));
        assertThatThrownBy(() -> servico.apagarDoTopico(10L, 20L, 7L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repositorio, never()).save(any());
    }

    private void prepararEdicao() {
        when(usuarios.buscarAtivo(7L)).thenReturn(autor);
        when(repositorio.findById(20L)).thenReturn(Optional.of(comentario));
    }
}
