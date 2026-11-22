package com.cursovalido.backend.aulas.service;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VimeoClientTest {
    private MockRestServiceServer servidor;
    private RestClient rest;
    private VimeoClient cliente;
    private static final String URL = "https://api.vimeo.com/videos/123?fields=uri,user.uri";

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("https://api.vimeo.com");
        servidor = MockRestServiceServer.bindTo(builder).build();
        rest = builder.build();
        cliente = new VimeoClient(rest, "token-ficticio", "456");
    }

    @Test
    void aceitaSomenteProprietarioAutorizadoEEnviaTokenNoCabecalho() {
        servidor.expect(requestTo(URL)).andExpect(header("Authorization", "Bearer token-ficticio"))
                .andRespond(withSuccess("{\"uri\":\"/videos/123\",\"user\":{\"uri\":\"/users/456\"}}", MediaType.APPLICATION_JSON));
        cliente.validarVideo("123");
        servidor.verify();
    }

    @Test
    void recusaOutraContaMesmoQuandoVideoEstaAcessivel() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"uri\":\"/videos/123\",\"user\":{\"uri\":\"/users/789\"}}", MediaType.APPLICATION_JSON));
        erro(() -> cliente.validarVideo("123"), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "null", "{\"uri\":\"/videos/123\"}",
            "{\"uri\":\"/videos/123\",\"user\":{}}",
            "{\"uri\":\"/videos/999\",\"user\":{\"uri\":\"/users/456\"}}",
            "{\"uri\":\"/videos/123\",\"user\":{\"uri\":\"/users/456/outro\"}}" })
    void naoAceitaRespostaIncompletaOuDivergente(String corpo) {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
        erro(() -> cliente.validarVideo("123"), 502);
    }

    @ParameterizedTest
    @ValueSource(ints = { 401, 403, 429, 500, 503 })
    void falhasExternasBloqueiamValidacao(int status) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));
        erro(() -> cliente.validarVideo("123"), 503);
    }

    @Test
    void videoInexistenteOuInacessivelEhRecusado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        erro(() -> cliente.validarVideo("123"), 400);
    }

    @Test
    void timeoutNaoLiberaVideo() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException()));
        erro(() -> cliente.validarVideo("123"), 503);
    }

    @Test
    void jsonInvalidoNaoLiberaVideo() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("json quebrado", MediaType.APPLICATION_JSON));
        erro(() -> cliente.validarVideo("123"), 503);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "perfil", "/users/456", "456/789" })
    void configuracaoInvalidaNaoConsultaAPI(String conta) {
        erro(() -> new VimeoClient(rest, "token-ficticio", conta).validarVideo("123"), 503);
        servidor.verify();
    }

    @Test
    void tokenAusenteNaoConsultaAPI() {
        erro(() -> new VimeoClient(rest, "", "456").validarVideo("123"), 503);
        servidor.verify();
    }
}
