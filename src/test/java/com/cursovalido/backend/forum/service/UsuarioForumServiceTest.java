package com.cursovalido.backend.forum.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import com.cursovalido.backend.forum.repository.UsuarioForumRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioForumServiceTest {
    @Mock private UsuarioForumRepository repositorio;
    @InjectMocks private UsuarioForumService servico;

    @Test
    void buscarAtivo_comUsuarioAtivo_deveRetornarUsuario() {
        var usuario = new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO");
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario));
        assertThat(servico.buscarAtivo(7L)).isSameAs(usuario);
    }

    @Test
    void buscarAtivo_semId_deveRejeitarSemConsultarBanco() {
        assertThatThrownBy(() -> servico.buscarAtivo(null)).isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void buscarAtivo_comIdInexistente_deveRetornarNaoEncontrado() {
        when(repositorio.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.buscarAtivo(7L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarAtivo_comUsuarioInativo_deveRetornarNaoEncontrado() {
        var usuario = new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO");
        usuario.setAtivo(false);
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> servico.buscarAtivo(7L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarAtivos_comUsuarios_deveRetornarConsultaDoRepositorio() {
        var usuarios = List.of(new UsuarioForum(7L, "Aluno", "aluno@teste.com", "ALUNO"));
        when(repositorio.findByAtivoTrueOrderByIdAsc()).thenReturn(usuarios);
        assertThat(servico.listarAtivos()).containsExactlyElementsOf(usuarios);
    }

    @Test
    void sincronizar_comUsuarioPrincipal_deveEnviarDadosEEstadoAoRepositorio() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        when(usuario.getNomeCompleto()).thenReturn("Nome atualizado");
        when(usuario.getEmail()).thenReturn("novo@teste.com");
        when(usuario.getPerfil()).thenReturn(Perfil.PROFESSOR);
        when(usuario.isAtivo()).thenReturn(false);
        servico.sincronizar(usuario);
        verify(repositorio).sincronizar(7L, "Nome atualizado", "novo@teste.com", "PROFESSOR", false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "administrador", "Administrador"})
    void ehAdministrador_comPerfilAdministrador_deveRetornarVerdadeiro(String perfil) {
        assertThat(servico.ehAdministrador(new UsuarioForum(7L, "Nome", "email@teste.com", perfil))).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"ALUNO", "PROFESSOR", ""})
    void ehAdministrador_comOutroPerfil_deveRetornarFalso(String perfil) {
        assertThat(servico.ehAdministrador(new UsuarioForum(7L, "Nome", "email@teste.com", perfil))).isFalse();
    }
}
