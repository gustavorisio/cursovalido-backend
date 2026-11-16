package com.cursovalido.backend.notificacoes.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.notificacoes.dto.ComunicadoRequest;
import com.cursovalido.backend.notificacoes.dto.ComunicadoResponse;
import com.cursovalido.backend.notificacoes.repository.ComunicadoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComunicadoServiceTest {

    private final ComunicadoRepository comunicados = mock(ComunicadoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final ComunicadoService service = new ComunicadoService(comunicados, usuarios, auditoria);
    private final ComunicadoResponse comunicado = new ComunicadoResponse(
            5L, "Aviso", "Aula adiada", LocalDateTime.of(2026, 1, 10, 9, 0), false);

    @Test
    void devePublicarComunicadoERegistrarAuditoria() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        when(comunicados.criar("Aviso", "Aula adiada", 1L)).thenReturn(comunicado);

        assertEquals(comunicado, service.publicar(1L, new ComunicadoRequest(" Aviso ", " Aula adiada ")));
        verify(auditoria).registrar(1L, "PUBLICACAO_COMUNICADO", "Comunicado 5", null);
    }

    @Test
    void deveEditarComunicadoExistente() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);
        when(comunicados.editar(5L, "Aviso", "Aula adiada")).thenReturn(Optional.of(comunicado));

        assertEquals(comunicado, service.editar(1L, 5L, new ComunicadoRequest("Aviso", "Aula adiada")));
        verify(auditoria).registrar(1L, "EDICAO_COMUNICADO", "Comunicado 5", null);
    }

    @Test
    void deveRetornarNaoEncontradoAoExcluirComunicadoInexistente() {
        cadastrarUsuario(Perfil.ADMINISTRADOR);

        var erro = assertThrows(ResponseStatusException.class, () -> service.excluir(1L, 5L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verifyNoInteractions(auditoria);
    }

    @Test
    void deveCalcularPaginasEIncluirTotalDeNotificacoesNaoLidas() {
        cadastrarUsuario(Perfil.ALUNO);
        when(comunicados.contar()).thenReturn(11L);
        when(comunicados.contarNaoLidas(1L)).thenReturn(3L);
        when(comunicados.listar(1L, 1, 10)).thenReturn(List.of(comunicado));

        var pagina = service.listarNotificacoes(1L, 1, 10);

        assertEquals(List.of(comunicado), pagina.itens());
        assertEquals(2, pagina.totalPaginas());
        assertEquals(3, pagina.naoLidas());
    }

    @Test
    void deveMarcarComunicadoComoLidoParaOUsuario() {
        cadastrarUsuario(Perfil.ALUNO);
        when(comunicados.existe(5L)).thenReturn(true);

        service.marcarLida(1L, 5L);

        verify(comunicados).marcarLida(5L, 1L);
    }

    @Test
    void deveRecusarPublicacaoPorAluno() {
        cadastrarUsuario(Perfil.ALUNO);

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.publicar(1L, new ComunicadoRequest("Aviso", "Aula adiada")));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(comunicados);
    }

    private void cadastrarUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }
}
