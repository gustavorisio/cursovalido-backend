package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.validation.login.ProtecaoForcaBrutaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.configuration.HttpsFilter;
import com.cursovalido.backend.authentication.configuration.LimiteRequisicoesFilter;
import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltrosSegurancaTest {
    @ParameterizedTest
    @CsvSource({
            "POST, /api/autenticacao/login, 429",
            "GET, /termos-aceite.html, 429",
            "GET, /politica-privacidade.html, 429",
            "DELETE, /api/autenticacao/conta, 429",
            "POST, /api/convites/completar-cadastro, 429",
            "POST, /api/administracao/convites, 429",
            "POST, /api/administracao/convites/1/reenviar, 429",
            "DELETE, /api/administracao/convites/1/cancelar, 429",
            "GET, /api/administracao/convites, 200",
            "DELETE, /api/convites/1, 429",
            "GET, /api/autenticacao/meus-dados, 200",
            "PUT, /api/convites/1, 429",
            "POST, /api/forum/topicos, 429",
            "PUT, /api/forum/topicos/1, 429",
            "DELETE, /api/forum/comentarios/1, 429"
    })
    void limitaAlteracoesDeDadosEAutenticacao(String metodo, String caminho, int statusEsperado) throws Exception {
        var filtro = new LimiteRequisicoesFilter(1);
        var cadeia = mock(FilterChain.class);
        filtro.doFilter(new MockHttpServletRequest(metodo, caminho), new MockHttpServletResponse(), cadeia);

        var resposta = new MockHttpServletResponse();
        filtro.doFilter(new MockHttpServletRequest(metodo, caminho), resposta, cadeia);

        assertEquals(statusEsperado, resposta.getStatus());
    }

    @Test
    void errosDosFiltrosUsamJsonSemDadosDaRequisicao() throws Exception {
        var cadeia = mock(FilterChain.class);
        var requisicao = new MockHttpServletRequest("POST", "/api/autenticacao/login");
        requisicao.setQueryString("segredo=valor-privado");
        var https = new MockHttpServletResponse();
        new HttpsFilter(true).doFilter(requisicao, https, cadeia);
        assertTrue(https.getContentType().startsWith("application/json"));
        assertEquals("no-store", https.getHeader("Cache-Control"));
        assertTrue(https.getContentAsString().contains("HTTPS_OBRIGATORIO"));
        assertFalse(https.getContentAsString().contains("valor-privado"));
        assertNull(https.getErrorMessage());

        var limite = new LimiteRequisicoesFilter(1);
        limite.doFilter(requisicao, new MockHttpServletResponse(), cadeia);
        var bloqueada = new MockHttpServletResponse();
        limite.doFilter(requisicao, bloqueada, cadeia);
        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        assertTrue(bloqueada.getContentAsString().contains("LIMITE_REQUISICOES_ATINGIDO"));
        assertFalse(bloqueada.getContentAsString().contains("/api/"));
        assertNull(bloqueada.getErrorMessage());
    }

    @Test
    void httpsOpcionalPermiteHttp() throws Exception {
        var cadeia = mock(FilterChain.class);
        var requisicao = new MockHttpServletRequest();
        var resposta = new MockHttpServletResponse();

        new HttpsFilter(false).doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        assertEquals(200, resposta.getStatus());
    }

    @Test
    void httpsObrigatorioBloqueiaHttp() throws Exception {
        var cadeia = mock(FilterChain.class);
        var resposta = new MockHttpServletResponse();
        new HttpsFilter(true).doFilter(new MockHttpServletRequest(), resposta, cadeia);
        assertEquals(426, resposta.getStatus());
        verifyNoInteractions(cadeia);
    }

    @Test
    void httpsObrigatorioPermiteConexaoSegura() throws Exception {
        var cadeia = mock(FilterChain.class);
        var requisicao = new MockHttpServletRequest();
        requisicao.setSecure(true);
        var resposta = new MockHttpServletResponse();
        new HttpsFilter(true).doFilter(requisicao, resposta, cadeia);
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void limitaPedidosPorIpEPermiteOutroIp() throws Exception {
        var filtro = new LimiteRequisicoesFilter();
        var cadeia = mock(FilterChain.class);
        for (int i = 0; i < 31; i++) {
            var requisicao = new MockHttpServletRequest("POST", "/api/autenticacao/login");
            requisicao.setRemoteAddr("192.0.2.1");
            var resposta = new MockHttpServletResponse();
            filtro.doFilter(requisicao, resposta, cadeia);
            assertEquals(i < 30 ? 200 : 429, resposta.getStatus());
        }
        var outra = new MockHttpServletRequest("POST", "/api/autenticacao/login");
        outra.setRemoteAddr("192.0.2.2");
        var resposta = new MockHttpServletResponse();
        filtro.doFilter(outra, resposta, cadeia);
        assertEquals(200, resposta.getStatus());
    }

    @Test
    void cincoFalhasBloqueiamConta() {
        var protecao = new ProtecaoForcaBrutaService();
        for (int i = 0; i < 5; i++)
            protecao.registrarFalha("aluno@example.com");
        assertThrows(AcessoTemporariamenteBloqueadoException.class,
                () -> protecao.verificar("aluno@example.com"));
        protecao.registrarSucesso("aluno@example.com");
        assertDoesNotThrow(() -> protecao.verificar("aluno@example.com"));
    }
}
