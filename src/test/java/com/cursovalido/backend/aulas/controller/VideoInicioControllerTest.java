package com.cursovalido.backend.aulas.controller;

import com.cursovalido.backend.aulas.dto.*;
import com.cursovalido.backend.aulas.entity.*;
import com.cursovalido.backend.aulas.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.cursovalido.backend.support.ControllerFixtures.*;

class VideoInicioControllerTest {
    @Test
    void consultaPublicaPreservaAusenciaOuLinkComPrivacidade() {
        var servico = mock(VideoInicioService.class);
        var controlador = new VideoInicioController(servico);
        when(servico.consultar()).thenReturn(new VideoInicioResponse(null),
                new VideoInicioResponse("https://vimeo.com/123/abc"));
        assertThat(controlador.consultar().vimeoUrl()).isNull();
        assertThat(controlador.consultar().vimeoUrl()).isEqualTo("https://vimeo.com/123/abc");
    }
}
