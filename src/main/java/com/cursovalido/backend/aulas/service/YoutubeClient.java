package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.YoutubeConsultaResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class YoutubeClient {
    private final RestClient cliente;
    private final String chaveApi;
    private final String canalAutorizado;

    @Autowired
    public YoutubeClient(@Value("${external.youtube.base-url:https://www.googleapis.com/youtube/v3}") String url,
            @Value("${external.youtube.api-key:}") String chaveApi,
            @Value("${external.youtube.allowed-channel-id:}") String canalAutorizado,
            @Value("${external.youtube.connect-timeout-ms:3000}") int conexaoMs,
            @Value("${external.youtube.read-timeout-ms:5000}") int leituraMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(conexaoMs)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofMillis(leituraMs));
        this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
        this.chaveApi = chaveApi;
        this.canalAutorizado = canalAutorizado == null ? "" : canalAutorizado.trim();
    }

    // Permite testar o cliente HTTP sem chamar o YouTube real.
    YoutubeClient(RestClient cliente, String chaveApi, String canalAutorizado) {
        this.cliente = cliente;
        this.chaveApi = chaveApi;
        this.canalAutorizado = canalAutorizado == null ? "" : canalAutorizado.trim();
    }

    public void validarVideo(String videoId) {
        if (chaveApi.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Integracao com YouTube nao configurada");
        }
        if (!canalAutorizado.matches("UC[A-Za-z0-9_-]{22}")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Canal autorizado do YouTube nao configurado ou invalido (YOUTUBE_ALLOWED_CHANNEL_ID)");
        }
        YoutubeConsultaResponse resposta;
        try {
            resposta = cliente.get()
                    .uri(uri -> uri.path("/videos").queryParam("part", "status,snippet")
                            .queryParam("id", videoId).build())
                    .header("X-Goog-Api-Key", chaveApi)
                    .retrieve().body(YoutubeConsultaResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Consulta ao YouTube indisponivel: verifique a quota e as permissoes da chave");
            }
            if (ex.getStatusCode().value() == 429) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Limite de consultas ao YouTube atingido");
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "YouTube temporariamente indisponivel");
            }
            throw respostaInvalida();
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel consultar o YouTube no tempo disponivel");
        } catch (RestClientException ex) {
            throw respostaInvalida();
        }
        verificarResposta(resposta, videoId);
    }

    private void verificarResposta(YoutubeConsultaResponse resposta, String videoId) {
        if (resposta == null) {
            throw respostaInvalida();
        }
        if (resposta.items() == null) {
            throw respostaInvalida();
        }
        if (resposta.items().isEmpty()) {
            throw videoInvalido("Video nao encontrado ou nao esta acessivel no YouTube");
        }
        if (resposta.items().size() != 1) {
            throw respostaInvalida();
        }
        var video = resposta.items().getFirst();
        if (video == null) {
            throw respostaInvalida();
        }
        if (!videoId.equals(video.id())) {
            throw respostaInvalida();
        }
        if (video.status() == null) {
            throw respostaInvalida();
        }
        var status = video.status();
        if (status.embeddable() == null) {
            throw respostaInvalida();
        }
        if (!status.embeddable()) {
            throw videoInvalido("O video nao permite reproducao em outros sites");
        }
        if (status.privacyStatus() == null) {
            throw respostaInvalida();
        }
        if (!List.of("public", "unlisted").contains(status.privacyStatus())) {
            throw videoInvalido("Use um video publico ou nao listado");
        }
        if (status.uploadStatus() == null) {
            throw respostaInvalida();
        }
        if (!"processed".equals(status.uploadStatus())) {
            throw videoInvalido("O video ainda nao esta pronto para reproducao");
        }
        if (video.snippet() == null || video.snippet().channelId() == null
                || video.snippet().channelId().isBlank()) {
            throw respostaInvalida();
        }
        if (!canalAutorizado.equals(video.snippet().channelId())) {
            throw videoInvalido("O video deve pertencer ao canal autorizado da plataforma");
        }
    }

    private ResponseStatusException videoInvalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseStatusException respostaInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta invalida na consulta ao YouTube");
    }

}
