package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

@ExtendWith(MockitoExtension.class)
class AulaServiceTest {
    @Mock private AulaRepository repositorio;
    @Mock private ModuloService modulos;
    @Mock private YoutubeService youtube;
    @Mock private LogAuditoriaService auditoria;
    @Mock private ProgressoAulaRepository progressos;
    @InjectMocks private AulaService servico;
    private final AulaRequest dados = new AulaRequest(" Java ", " Basico ", 2, "https://youtu.be/M7lc1UVf-VE");

    @Test
    void criarAutorizaEValidaVideoAntesDeSalvar() {
        when(youtube.validarVideo(dados.youtubeUrl())).thenReturn("M7lc1UVf-VE");
        var salvo = servico.criar(20L, dados, 1L);
        assertThat(salvo.getTitulo()).isEqualTo("Java");
        assertThat(salvo.getDescricao()).isEqualTo("Basico");
        assertThat(salvo.getOrdem()).isEqualTo(2);
        assertThat(salvo.getIdModulo()).isEqualTo(20L);
        assertThat(salvo.getYoutubeVideoId()).isEqualTo("M7lc1UVf-VE");
        assertThat(salvo.getCriadoEm()).isNotNull();
        var ordem = inOrder(modulos, youtube, repositorio, auditoria);
        ordem.verify(modulos).buscarAtivo(20L, 1L);
        ordem.verify(youtube).validarVideo(dados.youtubeUrl());
        ordem.verify(repositorio).save(salvo);
        ordem.verify(auditoria).registrar(eq(1L), eq("CADASTRO_AULA"), anyString(), isNull());
    }

    @Test
    void listarVerificaAcessoAoModulo() {
        var aula = aula();
        when(repositorio.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(20L)).thenReturn(List.of(aula));
        assertThat(servico.listar(20L, 1L)).containsExactly(aula);
        verify(modulos).buscarAtivo(20L, 1L);
    }

    @Test
    void editarMesmoVideoPreservaProgresso() {
        var aula = aula();
        when(repositorio.findById(30L)).thenReturn(Optional.of(aula));
        when(youtube.validarVideo(dados.youtubeUrl())).thenReturn(aula.getYoutubeVideoId());
        servico.editar(30L, dados, 1L);
        verifyNoInteractions(progressos);
        verify(auditoria).registrar(1L, "EDICAO_AULA", "Aula 30", null);
        assertThat(aula.getIdModulo()).isEqualTo(20L);
    }

    @Test
    void trocarVideoRemoveSomenteProgressoDaAula() {
        var aula = aula();
        when(repositorio.findById(30L)).thenReturn(Optional.of(aula));
        when(youtube.validarVideo(dados.youtubeUrl())).thenReturn("abcdefghijk");
        servico.editar(30L, dados, 1L);
        assertThat(aula.getYoutubeVideoId()).isEqualTo("abcdefghijk");
        verify(progressos).deleteByIdAula(30L);
    }

    @Test
    void falhaDoYoutubePreservaDadosEProgresso() {
        var aula = aula();
        when(repositorio.findById(30L)).thenReturn(Optional.of(aula));
        when(youtube.validarVideo(dados.youtubeUrl())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        erro(() -> servico.editar(30L, dados, 1L), 503);
        assertThat(aula.getOrdem()).isEqualTo(1);
        assertThat(aula.getYoutubeVideoId()).isEqualTo("M7lc1UVf-VE");
        verify(repositorio, never()).save(any());
        verifyNoInteractions(progressos, auditoria);
    }

    @Test
    void acessoNegadoNaoConsomeYoutube() {
        when(modulos.buscarAtivo(20L, 1L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        erro(() -> servico.criar(20L, dados, 1L), 403);
        verifyNoInteractions(youtube, repositorio, auditoria);
    }

    @Test
    void aulaAusenteRetorna404() {
        erro(() -> servico.buscarAtivo(30L, 1L), 404);
    }

    @Test
    void aulaInativaRetorna404() {
        var aula = aula();
        aula.setAtivo(false);
        when(repositorio.findById(30L)).thenReturn(Optional.of(aula));
        erro(() -> servico.buscarAtivo(30L, 1L), 404);
        verifyNoInteractions(modulos);
    }

    @Test
    void excluirNaoRemoveVideoNemProgresso() {
        var aula = aula();
        when(repositorio.findById(30L)).thenReturn(Optional.of(aula));
        servico.apagar(30L, 1L);
        assertThat(aula.isAtivo()).isFalse();
        verify(repositorio).save(aula);
        verify(auditoria).registrar(1L, "EXCLUSAO_AULA", "Aula 30", null);
        verifyNoInteractions(youtube, progressos);
    }
}
