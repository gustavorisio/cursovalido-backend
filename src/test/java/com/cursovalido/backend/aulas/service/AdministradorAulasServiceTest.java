package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.aulas.entity.Aula;
import com.cursovalido.backend.aulas.entity.Curso;
import com.cursovalido.backend.aulas.repository.AulaRepository;
import com.cursovalido.backend.aulas.repository.CursoRepository;
import com.cursovalido.backend.aulas.repository.ModuloRepository;
import com.cursovalido.backend.aulas.repository.ProgressoAulaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdministradorAulasServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CursoRepository cursos = mock(CursoRepository.class);
    private final ModuloRepository modulos = mock(ModuloRepository.class);
    private final AulaRepository aulas = mock(AulaRepository.class);
    private final YoutubeService youtube = mock(YoutubeService.class);
    private final VimeoService vimeo = spy(new VimeoService(mock(VimeoClient.class)));
    private final ProgressoAulaRepository progressos = mock(ProgressoAulaRepository.class);
    private final CursoService gestaoCursos = mock(CursoService.class);
    private final AdministradorAulasService service = new AdministradorAulasService(usuarios, cursos, modulos,
            aulas, youtube, vimeo, progressos, mock(LogAuditoriaService.class), gestaoCursos);

    @Test
    void deveListarCursosAtivosParaAdministrador() {
        cadastrarAdministrador();
        Curso curso = AulasTestSupport.curso();
        when(cursos.findByAtivoTrueOrderByIdDesc()).thenReturn(List.of(curso));

        assertEquals(List.of(curso), service.listarCursos(1L));
    }

    @Test
    void deveDesativarCursoAoExcluir() {
        cadastrarAdministrador();
        Curso curso = AulasTestSupport.curso();
        when(cursos.findById(10L)).thenReturn(Optional.of(curso));

        service.excluirCurso(10L, 1L);

        assertFalse(curso.isAtivo());
        verify(cursos).save(curso);
    }

    @Test
    void deveListarAulasDoModuloAtivo() {
        Aula aula = cadastrarAula();
        when(aulas.findByIdModuloOrderByOrdemAscIdAsc(20L)).thenReturn(List.of(aula));

        assertEquals(List.of(aula), service.listarAulas(20L, 1L));
    }

    @Test
    void deveApagarProgressoAoTrocarVideoDaAula() {
        Aula aula = cadastrarAula();
        var dados = new AulaRequest(" Novo título ", " Descrição ", 2, "https://youtu.be/abcdefghijk",
                "https://vimeo.com/123456789");
        when(youtube.validarVideo(dados.youtubeUrl())).thenReturn("abcdefghijk");

        service.editarAula(30L, dados, 1L);

        assertEquals("Novo título", aula.getTitulo());
        assertEquals("abcdefghijk", aula.getYoutubeVideoId());
        verify(progressos).deleteByIdAula(30L);
        verify(aulas).save(aula);
    }

    @Test
    void devePreservarAulaEProgressoQuandoVimeoRecusa() {
        Aula aula = cadastrarAula();
        var dados = new AulaRequest("Novo", "Descricao", 2, "https://youtu.be/abcdefghijk",
                "https://vimeo.com/123456789");
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST)).when(vimeo).validarVideo(dados.vimeoUrl());
        assertThrows(ResponseStatusException.class, () -> service.editarAula(30L, dados, 1L));
        assertEquals(1, aula.getOrdem());
        verify(aulas, never()).save(any());
        verifyNoInteractions(progressos, youtube);
    }

    @Test
    void deveArquivarAulaAtiva() {
        Aula aula = cadastrarAula();

        service.alterarDisponibilidade(30L, false, 1L);

        assertFalse(aula.isAtivo());
        verify(aulas).save(aula);
    }

    @Test
    void deveRecusarConsultaPorProfessor() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.PROFESSOR)));

        var erro = assertThrows(ResponseStatusException.class, () -> service.listarCursos(1L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(cursos);
    }

    private void cadastrarAdministrador() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(AulasTestSupport.usuario(Perfil.ADMINISTRADOR)));
    }

    private Aula cadastrarAula() {
        cadastrarAdministrador();
        when(cursos.findById(10L)).thenReturn(Optional.of(AulasTestSupport.curso()));
        when(modulos.findById(20L)).thenReturn(Optional.of(AulasTestSupport.modulo()));
        Aula aula = AulasTestSupport.aula();
        when(aulas.findById(30L)).thenReturn(Optional.of(aula));
        return aula;
    }
}
