package com.cursovalido.backend.authentication.configuration;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LimiteRequisicoesFilterTest {
    @Test
    void exportacaoEHeadCompartilhamIntervaloDoFiltroUnico() throws Exception {
        var filtro = new LimiteRequisicoesFilter(2, 2);
        assertThat(enviar(filtro, "GET", "/api/autenticacao/exportar-dados", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(enviar(filtro, "HEAD", "/api/autenticacao/exportar-dados", "192.0.2.1").getStatus()).isEqualTo(429);
        assertThat(enviar(filtro, "GET", "/api/autenticacao/exportar-dados", "192.0.2.1").getStatus()).isEqualTo(429);
        assertThat(enviar(filtro, "GET", "/api/autenticacao/exportar-dados", "192.0.2.2").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse enviar(LimiteRequisicoesFilter filtro, String metodo, String caminho, String ip)
            throws Exception {
        var request = new MockHttpServletRequest(metodo, caminho);
        request.setRemoteAddr(ip);
        var response = new MockHttpServletResponse();
        filtro.doFilter(request, response, (req, res) -> {
        });
        return response;
    }

    @Test
    void trocarIdsEEndpointsNaoReiniciaLimiteDoIP() throws Exception {
        var filtro = new LimiteRequisicoesFilter(2, 2);
        assertThat(enviar(filtro, "POST", "/api/forum/topicos/1/comentarios", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(enviar(filtro, "PUT", "/api/forum/comentarios/999", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(enviar(filtro, "POST", "/api/qualquer-rota/123", "192.0.2.1").getStatus()).isEqualTo(429);
        assertThat(enviar(filtro, "POST", "/api/forum/topicos", "192.0.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void ataqueComDezMilCaminhosNaoBloqueiaOutraOrigem() throws Exception {
        var filtro = new LimiteRequisicoesFilter(30, 5);
        for (int i = 0; i < 10000; i++)
            enviar(filtro, "POST", "/api/forum/topicos/" + i + "/comentarios", "192.0.2.1");
        assertThat(enviar(filtro, "POST", "/api/autenticacao/login", "192.0.2.2").getStatus()).isEqualTo(200);
        var cache = (Map<?, ?>) ReflectionTestUtils.getField(filtro, "janelas");
        assertThat(cache).hasSize(2);
    }

    @Test
    void cacheCheioNaoRecusaNovoIP() throws Exception {
        var filtro = new LimiteRequisicoesFilter();
        for (int i = 0; i < 10001; i++)
            enviar(filtro, "POST", "/api/forum/topicos", "198.51." + i / 256 + "." + i % 256);
        assertThat(enviar(filtro, "POST", "/api/autenticacao/login", "192.0.2.2").getStatus()).isEqualTo(200);
        var cache = (Map<?, ?>) ReflectionTestUtils.getField(filtro, "janelas");
        assertThat(cache).hasSize(10000);
    }

    @Test
    void limitesDeAutenticacaoLeituraEExpiracaoContinuamAtivos() throws Exception {
        var relogio = mock(Clock.class);
        var inicio = Instant.parse("2026-11-22T12:00:00Z");
        when(relogio.instant()).thenReturn(inicio);
        var filtro = new LimiteRequisicoesFilter(3, 1, relogio);
        assertThat(enviar(filtro, "POST", "/api/autenticacao/login", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(enviar(filtro, "POST", "/api/autenticacao/login", "192.0.2.1").getStatus()).isEqualTo(429);
        assertThat(enviar(filtro, "GET", "/api/forum/topicos/1/comentarios", "192.0.2.1").getStatus()).isEqualTo(200);
        assertThat(enviar(filtro, "GET", "/api/forum/topicos/2/comentarios", "192.0.2.1").getStatus()).isEqualTo(200);
        when(relogio.instant()).thenReturn(inicio.plusSeconds(59));
        var bloqueada = enviar(filtro, "GET", "/api/forum/users", "192.0.2.1");
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("1");
        when(relogio.instant()).thenReturn(inicio.plusSeconds(60));
        assertThat(enviar(filtro, "GET", "/api/forum/users", "192.0.2.1").getStatus()).isEqualTo(200);
    }
}
