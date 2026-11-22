package com.cursovalido.backend.aulas.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

class VimeoServiceTest {
    private final VimeoClient cliente = mock(VimeoClient.class);
    private final VimeoService servico = new VimeoService(cliente);

    @ParameterizedTest
    @ValueSource(strings = { "https://vimeo.com/123", "https://www.vimeo.com/123/abcdef0123",
            "https://vimeo.com/123/4567890123", "https://player.vimeo.com/video/123?h=abc123",
            " https://vimeo.com/123 " })
    void consultaExatamenteOVideoDoPlayerEPreservaHash(String url) {
        assertThat(servico.validarVideo(url)).isEqualTo(url.trim());
        verify(cliente).validarVideo("123");
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://vimeo.com/123", "https://vimeo.com.evil.com/123",
            "https://vimeo.com@evil.com/123", "https://vimeo.com:443/123", "https://vimeo.com/abc",
            "https://player.vimeo.com/123", "https://vimeo.com/123/abc/999", "https://vimeo.com/%31%32%33",
            "https://vimeo.com/123%2f456", "https://player.vimeo.com/video/123/456" })
    void urlInvalidaNaoConsultaAPI(String url) {
        erro(() -> servico.validarVideo(url), 400);
        verifyNoInteractions(cliente);
    }

    @Test
    void vazioRemoveVideoSemConsultarAPI() {
        assertThat(servico.validarVideo(null)).isNull();
        assertThat(servico.validarVideo("  ")).isNull();
        verifyNoInteractions(cliente);
    }
}
