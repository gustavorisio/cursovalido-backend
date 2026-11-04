package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.configuration.HttpsFiltro;
import com.cursovalido.backend.authentication.configuration.LimiteRequisicoesFiltro;
import com.cursovalido.backend.authentication.exception.AcessoTemporariamenteBloqueadoException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltrosSegurancaTest {
    @Test
    void httpsObrigatorioBloqueiaHttp() throws Exception {
        var cadeia = mock(FilterChain.class);
        var resposta = new MockHttpServletResponse();
        new HttpsFiltro(true).doFilter(new MockHttpServletRequest(), resposta, cadeia);
        assertEquals(426, resposta.getStatus());
        verifyNoInteractions(cadeia);
    }

    @Test
    void httpsObrigatorioPermiteConexaoSegura() throws Exception {
        var cadeia = mock(FilterChain.class);
        var requisicao = new MockHttpServletRequest();
        requisicao.setSecure(true);
        var resposta = new MockHttpServletResponse();
        new HttpsFiltro(true).doFilter(requisicao, resposta, cadeia);
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void limitaPedidosPorIpEPermiteOutroIp() throws Exception {
        var filtro = new LimiteRequisicoesFiltro();
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
        var protecao = new ProtecaoForcaBrutaServico();
        for (int i = 0; i < 5; i++)
            protecao.registrarFalha("aluno@example.com");
        assertThrows(AcessoTemporariamenteBloqueadoException.class,
                () -> protecao.verificar("aluno@example.com"));
        protecao.registrarSucesso("aluno@example.com");
        assertDoesNotThrow(() -> protecao.verificar("aluno@example.com"));
    }
}
