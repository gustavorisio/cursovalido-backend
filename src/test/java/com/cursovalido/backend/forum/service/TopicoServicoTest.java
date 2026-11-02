package com.cursovalido.backend.forum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicoServicoTest {

    @Mock
    private TopicoRepositorio repositorio;
    @Mock
    private UsuarioForumServico usuarios;
    @Mock
    private LogAuditoriaServico auditoria;
    @InjectMocks
    private TopicoServico servico;

    private UsuarioForum usuario;

    @BeforeEach
    void configurarCenario() {
        // O mesmo usuário ativo é usado nos cenários que dependem de autoria.
        usuario = new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO");
    }

    @Test
    void deveCriarTopicoERegistrarAuditoria() {
        Topico topico = new Topico();
        topico.setTitulo("Java");
        topico.setDescricao("Duvida");
        when(usuarios.buscarAtivo(7L)).thenReturn(usuario);
        when(repositorio.save(topico)).thenReturn(topico);

        Topico resultado = servico.criar(topico, 7L);

        assertEquals(7L, resultado.getIdAutor());
        verify(repositorio).save(topico);
        verify(auditoria).registrar(eq(7L), eq("CADASTRO_TOPICO"), any(String.class), eq(null));
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoForAutor() {
        Topico topico = new Topico();
        topico.setIdAutor(99L);
        // O serviço deve negar a edição sem chegar ao repositório de gravação.
        when(usuarios.buscarAtivo(7L)).thenReturn(usuario);
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));

        assertThrows(AcessoNegadoException.class,
                () -> servico.editar(10L, new Topico(), 7L));
        verify(repositorio, never()).save(any(Topico.class));
    }

    @Test
    void deveLancarExcecaoQuandoTopicoEstiverArquivado() {
        Topico topico = new Topico();
        topico.setAtivo(false);
        when(repositorio.findById(10L)).thenReturn(Optional.of(topico));

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.buscarAtivo(10L));
    }

    @Test
    void deveListarTopicosComBuscaSemEspacosNasExtremidades() {
        when(repositorio.listarRecentes(any(), eq("java"))).thenReturn(List.of());

        servico.listarRecentes(0, 5, "  java  ", 7L);

        verify(repositorio).listarRecentes(any(), eq("java"));
        verify(auditoria).registrar(7L, "CONSULTA_TOPICOS", "java", null);
    }
}
