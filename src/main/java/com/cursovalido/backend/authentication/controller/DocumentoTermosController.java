package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.PrivacidadeService;
import com.cursovalido.backend.authentication.validation.politica.ConteudoDocumento;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequiredArgsConstructor
public class DocumentoTermosController {
    private final PoliticaService politicas;
    private final PrivacidadeService privacidade;

    @GetMapping(value = "/politica-privacidade.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> documentoPrivacidade() throws IOException {
        return responder("politica-privacidade.html", privacidade.buscarAtiva());
    }

    @GetMapping(value = "/termos-aceite.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> documento() throws IOException {
        return responder("termos-aceite.html", politicas.listarAtivas().stream().findFirst().orElse(null));
    }

    private ResponseEntity<String> responder(String recurso, PoliticaResponse politica) throws IOException {
        String modelo;
        try (var entrada = new ClassPathResource("static/" + recurso).getInputStream()) {
            modelo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (politica == null) {
            return ResponseEntity.notFound().build();
        }
        String html = renderizarDocumento(modelo, politica);
        return ResponseEntity.ok().header("Cache-Control", "public, max-age=60")
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
    }

    static String renderizarDocumento(String modelo, PoliticaResponse politica) {
        var navegacao = Pattern.compile("(?is)<nav\\b[^>]*>.*?</nav>").matcher(modelo);
        String botoes = navegacao.find() ? navegacao.group() : "";
        String conteudo = renderizarConteudo(politica.conteudo());
        String principal = "<main>" + botoes + "<h1>" + HtmlUtils.htmlEscape(politica.nome())
                + "</h1><p class=\"atualizacao\"><strong>Versão: " + HtmlUtils.htmlEscape(politica.versao())
                + "</strong></p><article style=\"overflow-wrap:anywhere;white-space:pre-wrap\">" + conteudo + "</article></main>";
        return modelo.replaceFirst("(?is)<main\\b[^>]*>.*?</main>",
                Matcher.quoteReplacement(principal));
    }

    static String renderizarConteudo(String conteudo) {
        if (conteudo == null)
            return "";
        return HtmlUtils.htmlEscape(ConteudoDocumento.preparar(conteudo));
    }
}
