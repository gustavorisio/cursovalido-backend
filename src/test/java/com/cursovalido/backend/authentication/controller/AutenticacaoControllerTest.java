package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.AceiteTermosRequest;
import com.cursovalido.backend.authentication.dto.AutenticacaoResponse;
import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.dto.Codigo2faRequest;
import com.cursovalido.backend.authentication.dto.DecisaoTermosRequest;
import com.cursovalido.backend.authentication.dto.EmailRequest;
import com.cursovalido.backend.authentication.dto.ExclusaoContaRequest;
import com.cursovalido.backend.authentication.dto.LoginRequest;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.service.AutenticacaoService;
import com.cursovalido.backend.authentication.service.CadastroUsuarioService;
import com.cursovalido.backend.authentication.service.ContaUsuarioService;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.SenhaUsuarioService;
import com.cursovalido.backend.authentication.service.TermosUsuarioService;
import com.cursovalido.backend.authentication.service.TokenJwtService;
import java.time.LocalDate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoControllerTest {

    private static final String EMAIL = "aluno@teste.com";
    private static final String IP = "192.0.2.10";
    private final UsuarioAutenticado usuario = new UsuarioAutenticado(1L, EMAIL, "hash", Perfil.ALUNO, true, true, true);
    private final AutenticacaoService autenticacao = mock(AutenticacaoService.class);
    private final CadastroUsuarioService cadastros = mock(CadastroUsuarioService.class);
    private final TermosUsuarioService termos = mock(TermosUsuarioService.class);
    private final ContaUsuarioService contas = mock(ContaUsuarioService.class);
    private final TokenJwtService tokens = mock(TokenJwtService.class);
    private final LogAuditoriaService auditoria = mock(LogAuditoriaService.class);
    private final AutenticacaoController controller = new AutenticacaoController(cadastros,
            mock(SenhaUsuarioService.class), termos, contas, autenticacao, tokens, auditoria);

    @Test
    void deveIndicarNecessidadeDeSegundoFatorNoLogin() {
        var dados = new LoginRequest(EMAIL, "senha");
        when(autenticacao.autenticar(dados, IP))
                .thenReturn(new AutenticacaoResponse(null, null, null, null, true));

        assertTrue(controller.login(dados, requisicao()).requerCodigo2fa());
    }

    @Test
    void deveRetornarTokenAoConfirmarCodigo() {
        when(autenticacao.confirmarCodigo(EMAIL, "123456", IP))
                .thenReturn(new AutenticacaoResponse("token", 1L, "Aluno", Perfil.ALUNO, false));

        var sessao = controller.confirmar2fa(new Codigo2faRequest(EMAIL, "123456"), requisicao());

        assertEquals("token", sessao.token());
    }

    @Test
    void deveEncaminharIpAoReenviarCodigo() {
        controller.reenviar2fa(new EmailRequest(EMAIL), requisicao());

        verify(autenticacao).reenviarCodigo2fa(EMAIL, IP);
    }

    @Test
    void deveEncaminharIpAoCadastrarUsuario() {
        var dados = new CadastroUsuarioRequest("Aluno Teste", EMAIL, "Senha@123",
                LocalDate.now().minusYears(25), true, true, "v1.2");

        controller.cadastrar(dados, requisicao());

        verify(cadastros).cadastrar(dados, IP);
    }

    @Test
    void deveExportarDadosComoAnexoSemCache() {
        var resposta = controller.exportarDados(usuario);

        assertEquals("attachment; filename=meus-dados.json", resposta.getHeaders().getFirst("Content-Disposition"));
        assertEquals("no-store", resposta.getHeaders().getCacheControl());
        verify(contas).consultarDadosParaExportacao(1L);
        verify(auditoria).registrar(1L, "EXPORTACAO_DADOS", "Dados de cadastro exportados", null);
    }

    @Test
    void deveEncerrarTokenNoLogout() {
        controller.logout("Bearer token", usuario);

        verify(tokens).encerrar("token");
        verify(auditoria).registrar(1L, "LOGOUT", "Sessao encerrada", null);
    }

    @Test
    void deveEncaminharRecusaDosTermosComTokenDaSessao() {
        controller.decidirTermos(usuario, "Bearer token", new DecisaoTermosRequest(false), requisicao());

        verify(termos).encerrarPorDecisao(1L, "token", false, IP);
    }

    @Test
    void deveRetornarNovaSessaoSemCacheAposAceite() {
        var dados = new AceiteTermosRequest("v1.2");
        var sessao = new AutenticacaoResponse("novo-token", 1L, "Aluno", Perfil.ALUNO, false);
        when(termos.confirmarAceite(1L, "token-termos", dados, IP)).thenReturn(sessao);

        var resposta = controller.aceitarTermosNovamente(usuario, "Bearer token-termos", dados, requisicao());

        assertEquals(sessao, resposta.getBody());
        assertEquals("no-store", resposta.getHeaders().getCacheControl());
    }

    @Test
    void deveEncaminharCodigoDeExclusaoDaContaAutenticada() {
        controller.excluirConta(usuario, new ExclusaoContaRequest("123456"));

        verify(contas).excluirConta(1L, "123456");
    }

    private MockHttpServletRequest requisicao() {
        var requisicao = new MockHttpServletRequest();
        requisicao.setRemoteAddr(IP);
        return requisicao;
    }
}

