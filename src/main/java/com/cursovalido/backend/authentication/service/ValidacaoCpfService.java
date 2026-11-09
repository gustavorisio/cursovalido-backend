package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.exception.MenorDeIdadeException;
import com.cursovalido.backend.authentication.exception.ValidacaoCpfException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.util.Locale;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ValidacaoCpfService {
    private static final DateTimeFormatter NASCIMENTO = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String TOKEN_PUBLICO_TESTE = "5ae973d7a997af13f0aaf2bf60e65803";
    private final RestClient cliente;
    private final String token;
    private final ObjectMapper json = new ObjectMapper();

    public ValidacaoCpfService(
            @Value("${external.cpfcnpj.base-url:https://api.cpfcnpj.com.br}") String url,
            @Value("${external.cpfcnpj.token:}") String token,
            @Value("${external.cpfcnpj.connect-timeout-ms:3000}") int conexaoMs,
            @Value("${external.cpfcnpj.read-timeout-ms:60000}") int leituraMs) {
        this.token = token.strip();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(conexaoMs))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofMillis(leituraMs));
        cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    public void validar(String cpf, String nomeInformado, LocalDate nascimentoInformado) {
        if (cpf == null || !cpf.matches("[0-9]{11}") || nascimentoInformado == null
                || nomeInformado == null || nomeInformado.isBlank())
            throw dadosInvalidos();
        if (Period.between(nascimentoInformado, LocalDate.now()).getYears() < 18)
            throw new MenorDeIdadeException();
        // O token publico retorna dados ficticios e nunca pode liberar cadastros.
        if (token.isBlank() || token.equals(TOKEN_PUBLICO_TESTE))
            throw indisponivel();
        JsonNode resposta;
        try {
            String corpo = cliente.get().uri("/{token}/26/{cpf}", token, cpf)
                    .retrieve().body(String.class);
            resposta = corpo == null ? null : json.readTree(corpo);
        } catch (Exception erro) {
            // Nao propaga a excecao HTTP: sua URL contem o token e o CPF.
            throw indisponivel();
        }
        if (resposta == null || !resposta.isObject() || !resposta.path("status").isIntegralNumber())
            throw indisponivel();
        if (resposta.path("status").intValue() == 0) {
            int codigo = resposta.path("erroCodigo").asInt(-1);
            if (codigo == 100 || codigo == 101 || codigo == 102)
                throw dadosInvalidos();
            throw indisponivel();
        }
        if (resposta.path("status").intValue() != 1
                || resposta.path("pacoteUsado").asInt(-1) != 26)
            throw indisponivel();
        String retornado = resposta.path("cpf").asText("");
        if (!retornado.matches("(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})"))
            throw indisponivel();
        if (!cpf.equals(retornado.replaceAll("\\D", "")))
            throw indisponivel();
        LocalDate nascimento;
        try {
            nascimento = LocalDate.parse(resposta.path("nascimento").asText(""), NASCIMENTO);
        } catch (RuntimeException erro) {
            throw indisponivel();
        }
        if (Period.between(nascimento, LocalDate.now()).getYears() < 18)
            throw new MenorDeIdadeException();
        if (!nascimento.equals(nascimentoInformado))
            throw dadosInvalidos();
        if (!resposta.path("nome").isTextual() || resposta.path("nome").asText().isBlank())
            throw indisponivel();
        if (!normalizarNome(resposta.path("nome").asText()).equals(normalizarNome(nomeInformado)))
            throw dadosInvalidos();
        // Nao armazena nem retorna a resposta cadastral. Nao ha analise de credito
        // ou exigencia de situacao fiscal REGULAR para acesso educacional.
    }

    private String normalizarNome(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("(?U)\\s+", " ").strip().toUpperCase(Locale.ROOT);
    }

    private ValidacaoCpfException dadosInvalidos() {
        return new ValidacaoCpfException(HttpStatus.UNPROCESSABLE_ENTITY, "CPF_DADOS_INVALIDOS",
                "CPF, nome completo ou data de nascimento nao conferem. Informe os dados cadastrados no CPF.");
    }

    private ValidacaoCpfException indisponivel() {
        return new ValidacaoCpfException(HttpStatus.SERVICE_UNAVAILABLE, "CPF_CONSULTA_INDISPONIVEL",
                "A verificacao cadastral esta indisponivel. Tente novamente mais tarde. O cadastro nao foi concluido.");
    }
}
