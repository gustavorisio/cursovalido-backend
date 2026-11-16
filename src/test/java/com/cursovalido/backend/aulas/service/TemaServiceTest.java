package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.aulas.entity.Tema;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.TemaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemaServiceTest {

    private final TemaRepository temas = mock(TemaRepository.class);
    private final CursoRepository cursos = mock(CursoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final TemaService service = new TemaService(temas, cursos, usuarios, auditoria);

    @Test
    void deveListarTemasEmOrdemDeNome() {
        Tema tema = new Tema();
        when(temas.findAllByOrderByNomeAsc()).thenReturn(List.of(tema));

        assertEquals(List.of(tema), service.listar());
    }

    @Test
    void deveSalvarTemaComNomeLimpo() {
        cadastrarProfessor();

        Tema tema = service.salvar(null, " Java ", 1L);

        assertEquals("Java", tema.getNome());
        verify(temas).save(tema);
    }

    @Test
    void deveRecusarNomeDuplicado() {
        cadastrarProfessor();
        when(temas.existsByNomeIgnoreCaseAndIdNot("Java", 0L)).thenReturn(true);

        var erro = assertThrows(ResponseStatusException.class, () -> service.salvar(null, "Java", 1L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(temas, never()).save(any());
    }

    @Test
    void deveDesvincularCursosAoExcluirTema() {
        cadastrarProfessor();
        Tema tema = new Tema();
        when(temas.findById(5L)).thenReturn(Optional.of(tema));

        service.excluir(5L, 1L);

        verify(cursos).removerTema(5L);
        verify(temas).delete(tema);
    }

    @Test
    void deveRecusarAlteracaoPorUsuarioSemAcesso() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.salvar(null, "Java", 1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(temas);
    }

    private void cadastrarProfessor() {
        Usuario professor = new Usuario();
        professor.setPerfil(Perfil.PROFESSOR);
        professor.setAtivo(true);
        when(usuarios.findById(1L)).thenReturn(Optional.of(professor));
    }
}
