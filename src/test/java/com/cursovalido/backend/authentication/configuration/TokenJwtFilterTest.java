package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.TokenJwtService;
import com.cursovalido.backend.authentication.service.UsuarioDetalhesService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenJwtFilterTest {

    private static final String EMAIL = "aluno@teste.com";
    private static final String ROTA_TERMO = "/api/autenticacao/decidir-termos";
    private final TokenJwtService tokens = mock(TokenJwtService.class);
    private final UsuarioDetalhesService usuarios = mock(UsuarioDetalhesService.class);
    private final TokenJwtFilter filtro = new TokenJwtFilter(tokens, usuarios);
    private final FilterChain cadeia = mock(FilterChain.class);
    private final MockHttpServletResponse resposta = new MockHttpServletResponse();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveIgnorarTokenNaRotaPublicaDeLogin() throws Exception {
        var requisicao = requisicao("POST", "/api/autenticacao/login");

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        verifyNoInteractions(tokens);
    }

    @Test
    void deveContinuarSemAutenticacaoQuandoNaoHaCabecalho() throws Exception {
        var requisicao = new MockHttpServletRequest("GET", "/api/cursos");

        filtro.doFilter(requisicao, resposta, cadeia);

        verify(cadeia).doFilter(requisicao, resposta);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deveAutenticarSessaoComTermosVigentes() throws Exception {
        prepararConta(true, 1L);
        var requisicao = requisicao("GET", "/api/cursos");

        filtro.doFilter(requisicao, resposta, cadeia);

        assertEquals("ROLE_ALUNO", autoridade());
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void deveRejeitarSessaoVinculadaAOutroUsuario() throws Exception {
        prepararConta(true, 2L);

        filtro.doFilter(requisicao("GET", "/api/cursos"), resposta, cadeia);

        assertEquals(401, resposta.getStatus());
        assertTrue(resposta.getContentAsString().contains("SESSAO_INVALIDA"));
        verifyNoInteractions(cadeia);
    }

    @Test
    void deveBloquearContaInativa() throws Exception {
        prepararConta(false, 1L);

        filtro.doFilter(requisicao("GET", "/api/cursos"), resposta, cadeia);

        assertEquals(403, resposta.getStatus());
        verifyNoInteractions(cadeia);
    }

    @Test
    void devePedirReautenticacaoQuandoVersaoAceitaNaoEstaVigente() throws Exception {
        prepararContaDesatualizada(true, 1L);

        filtro.doFilter(requisicao("GET", "/api/cursos"), resposta, cadeia);

        assertEquals(403, resposta.getStatus());
        assertTrue(resposta.getContentAsString().contains("TERMOS_REAUTENTICACAO"));
        verifyNoInteractions(cadeia);
    }

    @Test
    void devePermitirDecisaoComAutoridadeRestritaQuandoVersaoEstiverDesatualizada() throws Exception {
        prepararContaDesatualizada(true, 1L);
        var requisicao = requisicao("POST", ROTA_TERMO);

        filtro.doFilter(requisicao, resposta, cadeia);

        assertEquals("TERMOS_REAUTENTICACAO", autoridade());
        verify(cadeia).doFilter(requisicao, resposta);
    }

    @Test
    void deveRestringirTokenDeTermosEmRotaComum() throws Exception {
        prepararConta(true, 1L);
        when(tokens.dadosParaRequisicao("token")).thenReturn(
                new TokenJwtService.DadosTokenRequisicao(EMAIL, 1L, "sessao", "TERMOS"));

        filtro.doFilter(requisicao("GET", "/api/cursos"), resposta, cadeia);

        assertEquals(403, resposta.getStatus());
        verifyNoInteractions(cadeia);
    }

    @Test
    void devePermitirDecisaoComTokenDeTermos() throws Exception {
        prepararConta(true, 1L);
        when(tokens.dadosParaRequisicao("token")).thenReturn(
                new TokenJwtService.DadosTokenRequisicao(EMAIL, 1L, "sessao", "TERMOS"));
        var requisicao = requisicao("POST", ROTA_TERMO);

        filtro.doFilter(requisicao, resposta, cadeia);

        assertEquals("TERMOS_PENDENTES", autoridade());
        verify(cadeia).doFilter(requisicao, resposta);
    }

    private MockHttpServletRequest requisicao(String metodo, String rota) {
        var requisicao = new MockHttpServletRequest(metodo, rota);
        requisicao.addHeader("Authorization", "Bearer token");
        return requisicao;
    }

    private void prepararConta(boolean ativa, Long idNoToken) {
        when(tokens.dadosParaRequisicao("token")).thenReturn(
                new TokenJwtService.DadosTokenRequisicao(EMAIL, idNoToken, "sessao", "ACESSO"));
        when(usuarios.loadUserForRequest(EMAIL, idNoToken, "sessao"))
                .thenReturn(new UsuarioAutenticado(1L, EMAIL, "hash", Perfil.ALUNO, true, true, ativa,
                        "v1.2", true, false));
    }

    private void prepararContaDesatualizada(boolean ativa, Long idNoToken) {
        prepararConta(ativa, idNoToken);
        when(usuarios.loadUserForRequest(EMAIL, idNoToken, "sessao"))
                .thenReturn(new UsuarioAutenticado(1L, EMAIL, "hash", Perfil.ALUNO, true, true, ativa,
                        "v1.2", false, false));
    }

    private String autoridade() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao.getAuthorities().iterator().next().getAuthority();
    }
}
