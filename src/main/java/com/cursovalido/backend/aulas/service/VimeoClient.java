package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.VimeoConsultaResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VimeoClient {
    private final RestClient cliente;
    private final String token;
    private final String contaAutorizada;

    @Autowired
    public VimeoClient(@Value("${external.vimeo.base-url:https://api.vimeo.com}") String url,
            @Value("${external.vimeo.access-token:}") String token,
            @Value("${external.vimeo.allowed-user-id:}") String contaAutorizada,
            @Value("${external.vimeo.connect-timeout-ms:3000}") int conexaoMs,
            @Value("${external.vimeo.read-timeout-ms:5000}") int leituraMs) {
        this(criarCliente(url, conexaoMs, leituraMs), token, contaAutorizada);
    }

    VimeoClient(RestClient cliente, String token, String contaAutorizada) {
        this.cliente = cliente;
        this.token = token == null ? "" : token.trim();
        this.contaAutorizada = contaAutorizada == null ? "" : contaAutorizada.trim();
    }

    private static RestClient criarCliente(String url, int conexaoMs, int leituraMs) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(conexaoMs)).build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofMillis(leituraMs));
        return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    public void validarVideo(String videoId) {
        if (token.isBlank() || !contaAutorizada.matches("[0-9]{1,20}")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Integração com Vimeo não configurada: verifique VIMEO_ACCESS_TOKEN e VIMEO_ALLOWED_USER_ID.");
        }
        VimeoConsultaResponse resposta;
        try {
            resposta = cliente.get()
                    .uri(uri -> uri.path("/videos/{id}").queryParam("fields", "uri,user.uri").build(videoId))
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(VimeoConsultaResponse.class);
        } catch (RestClientResponseException erro) {
            if (erro.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Vídeo não encontrado ou não acessível no Vimeo.");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível validar o vídeo no Vimeo. Verifique o token e tente novamente.");
        } catch (RestClientException erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível confirmar a conta do vídeo no Vimeo. Tente novamente.");
        }
        if (resposta == null || !("/videos/" + videoId).equals(resposta.uri())
                || resposta.user() == null || resposta.user().uri() == null
                || !resposta.user().uri().matches("/users/[0-9]{1,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta inválida na consulta ao Vimeo.");
        }
        if (!("/users/" + contaAutorizada).equals(resposta.user().uri())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O vídeo deve pertencer à conta autorizada do Vimeo da plataforma.");
        }
    }
}
