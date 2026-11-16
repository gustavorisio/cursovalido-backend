package com.cursovalido.backend.noticias.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.noticias.dto.NoticiaRequest;
import com.cursovalido.backend.noticias.entity.Noticia;
import com.cursovalido.backend.noticias.repository.NoticiaRepository;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticiaServiceTest {

    private final NoticiaRepository noticias = mock(NoticiaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final NoticiaService service = new NoticiaService(noticias, usuarios, auditoria);

    @Test
    void deveLimparBuscaAntesDeConsultar() {
        Noticia noticia = new Noticia();
        when(noticias.listarRecentes(PageRequest.of(0, 10), "Java")).thenReturn(List.of(noticia));

        assertEquals(List.of(noticia), service.listarRecentes(0, 10, " Java "));
    }

    @Test
    void deveRecusarPaginaInvalida() {
        var erro = assertThrows(ResponseStatusException.class, () -> service.listarRecentes(-1, 10, null));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(noticias);
    }

    @Test
    void deveCriarNoticiaComAutorETextoSemEspacosNasPontas() {
        cadastrarAdministrador(1L);

        Noticia noticia = service.criar(new NoticiaRequest(" Curso novo ", " Confira as aulas "), 1L);

        assertEquals("Curso novo", noticia.getTitulo());
        assertEquals("Confira as aulas", noticia.getConteudo());
        assertEquals(1L, noticia.getIdAutor());
        verify(noticias).save(noticia);
    }

    @Test
    void deveAtualizarNoticiaDoAutor() {
        cadastrarAdministrador(1L);
        Noticia noticia = noticiaDoAutor(1L);
        when(noticias.findById(5L)).thenReturn(Optional.of(noticia));

        service.editar(5L, new NoticiaRequest("Aviso", "Novo horário"), 1L);

        assertEquals("Aviso", noticia.getTitulo());
        assertEquals("Novo horário", noticia.getConteudo());
        verify(noticias).save(noticia);
    }

    @Test
    void deveDesativarNoticiaDoAutor() {
        cadastrarAdministrador(1L);
        Noticia noticia = noticiaDoAutor(1L);
        when(noticias.findById(5L)).thenReturn(Optional.of(noticia));

        service.apagar(5L, 1L);

        assertFalse(noticia.isAtivo());
        verify(noticias).save(noticia);
    }

    @Test
    void deveImpedirQueOutroAdministradorApagueNoticia() {
        cadastrarAdministrador(1L);
        when(noticias.findById(5L)).thenReturn(Optional.of(noticiaDoAutor(2L)));

        var erro = assertThrows(ResponseStatusException.class, () -> service.apagar(5L, 1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verify(noticias, never()).save(any());
    }

    private void cadastrarAdministrador(Long id) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfil.ADMINISTRADOR);
        usuario.setAtivo(true);
        when(usuarios.findById(id)).thenReturn(Optional.of(usuario));
    }

    private Noticia noticiaDoAutor(Long idAutor) {
        Noticia noticia = new Noticia();
        noticia.setIdAutor(idAutor);
        noticia.setAtivo(true);
        return noticia;
    }
}
