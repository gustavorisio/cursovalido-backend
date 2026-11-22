package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.VideoInicioRequest;
import com.cursovalido.backend.aulas.entity.VideoInicio;
import com.cursovalido.backend.aulas.repository.VideoInicioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

class VideoInicioServiceTest {
    private final VideoInicioRepository videos = mock(VideoInicioRepository.class);
    private final VimeoClient cliente = mock(VimeoClient.class);
    private final VimeoService vimeo = new VimeoService(cliente);
    private final PrivilegiosAdministrativosService privilegios = mock(PrivilegiosAdministrativosService.class);
    private final VideoInicioService servico = new VideoInicioService(videos, vimeo, privilegios);

    @Test
    void consultaProprietarioAntesDeSalvar() {
        var resposta = servico.atualizar(1L, new VideoInicioRequest("https://vimeo.com/123"));
        assertThat(resposta.vimeoUrl()).isEqualTo("https://vimeo.com/123");
        var ordem = inOrder(privilegios, cliente, videos);
        ordem.verify(privilegios).verificarPrincipal(1L);
        ordem.verify(cliente).validarVideo("123");
        ordem.verify(videos).findById((short) 1);
        ordem.verify(videos).save(any());
    }

    @Test
    void outraContaNaoAlteraVideoExistente() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST)).when(cliente).validarVideo("123");
        erro(() -> servico.atualizar(1L, new VideoInicioRequest("https://vimeo.com/123")), 400);
        verifyNoInteractions(videos);
    }

    @Test
    void podeRemoverMesmoSemVimeoDisponivel() {
        var video = new VideoInicio((short) 1, "https://vimeo.com/123");
        when(videos.findById((short) 1)).thenReturn(Optional.of(video));
        assertThat(servico.atualizar(1L, new VideoInicioRequest(" ")).vimeoUrl()).isNull();
        assertThat(video.getVimeoUrl()).isNull();
        verify(videos).save(video);
        verifyNoInteractions(cliente);
    }

    @Test
    void acessoNegadoNaoConsultaVimeo() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(privilegios).verificarPrincipal(1L);
        erro(() -> servico.atualizar(1L, new VideoInicioRequest("https://vimeo.com/123")), 403);
        verifyNoInteractions(cliente, videos);
    }
}
