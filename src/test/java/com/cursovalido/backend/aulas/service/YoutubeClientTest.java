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
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class YoutubeClientTest {
    private MockRestServiceServer servidor;
    private YoutubeClient cliente;
    private static final String ID = "M7lc1UVf-VE";
    private static final String URL = "https://www.googleapis.com/youtube/v3/videos?part=status&id=" + ID;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("https://www.googleapis.com/youtube/v3");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new YoutubeClient(builder.build(), "chave-ficticia");
    }

    @ParameterizedTest
    @ValueSource(strings = { "public", "unlisted" })
    void consultarVideoComChaveNoCabecalho(String privacidade) {
        servidor.expect(requestTo(URL)).andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", "chave-ficticia"))
                .andRespond(withSuccess(video(true, privacidade, "processed"), MediaType.APPLICATION_JSON));
        cliente.validarVideo(ID);
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"items\":[]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{\"embeddable\":false,\"privacyStatus\":\"public\",\"uploadStatus\":\"processed\"}}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{\"embeddable\":true,\"privacyStatus\":\"private\",\"uploadStatus\":\"processed\"}}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{\"embeddable\":true,\"privacyStatus\":\"public\",\"uploadStatus\":\"uploaded\"}}]}" })
    void rejeitarVideoInadequado(String corpo) {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
        esperarErro(400);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"items\":[null]}", "{\"items\":[{},{}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\"}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{\"embeddable\":true}}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{\"embeddable\":true,\"privacyStatus\":\"public\"}}]}",
            "{\"items\":[{\"id\":\"outro\"}]}",
            "{\"items\":[{\"id\":\"M7lc1UVf-VE\",\"status\":{}}]}", "json quebrado" })
    void tratarRespostaInvalida(String corpo) {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
        esperarErro(502);
    }

    @ParameterizedTest
    @ValueSource(ints = { 403, 429, 500, 503 })
    void tratarQuotaPermissaoEIndisponibilidade(int status) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status))
                .body("nao expor resposta externa nem chave-ficticia").contentType(MediaType.APPLICATION_JSON));
        esperarErro(503);
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401 })
    void tratarCredencialInvalidaSemExporResposta(int status) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)).body("chave-ficticia"));
        esperarErro(502);
    }

    @Test
    void tratarTimeout() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("chave-ficticia")));
        esperarErro(503);
    }

    @Test
    void tratarCorpoVazio() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess());
        esperarErro(502);
    }

    @Test
    void chaveAusenteNaoFazRequisicao() {
        cliente = new YoutubeClient(RestClient.create(), "");
        esperarErro(503);
    }

    private void esperarErro(int status) {
        assertThatThrownBy(() -> cliente.validarVideo(ID)).isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
            assertThat(erro.getStatusCode().value()).isEqualTo(status);
            assertThat(erro.getMessage()).doesNotContain("chave-ficticia");
            assertThat(erro.getCause()).isNull();
        });
        servidor.verify();
    }

    private String video(boolean incorporavel, String privacidade, String processamento) {
        return """
                {"etag":"ignorar","items":[{"id":"M7lc1UVf-VE","snippet":{"title":"Extra"},
                "status":{"embeddable":%s,"privacyStatus":"%s","uploadStatus":"%s"}}]}
                """.formatted(incorporavel, privacidade, processamento);
    }
}
