package com.cursovalido.backend.authentication.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentoTermosRenderizacaoTest {
    @Test
    void renderizaCorpoSemExibirCabecalhoHtml() {
        String html = DocumentoTermosController.renderizarConteudo(
                "<!DOCTYPE html><html><head><title>Termos</title></head><body><h2>Objeto</h2><p>Texto <strong>importante</strong></p></body></html>");
        assertEquals("<h2>Objeto</h2><p>Texto <strong>importante</strong></p>", html);
    }

    @Test
    void preservaTextoSimplesEEscapaConteudoAtivo() {
        assertEquals("Texto\nA &amp; B", DocumentoTermosController.renderizarConteudo("Texto\nA & B"));
        String html = DocumentoTermosController
                .renderizarConteudo("<script>alert(1)</script><p onclick=alert(1)>Texto</p>");
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<p onclick"));
    }

    @Test
    void versoesPublicadasPreservamBotoesEEstilosDoDocumento() throws Exception {
        for (String recurso : new String[] { "termos-aceite.html", "politica-privacidade.html" }) {
            String modelo;
            try (var entrada = new org.springframework.core.io.ClassPathResource("static/" + recurso)
                    .getInputStream()) {
                modelo = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            var politica = new com.cursovalido.backend.authentication.dto.PoliticaResponse("Documento", "1.0",
                    "<!DOCTYPE html><html><body><h1>Titulo antigo</h1><p><strong>Versao 1.0</strong></p><h2>Objeto</h2><p>Texto vigente</p></body></html>",
                    java.time.LocalDateTime.now());
            String html = DocumentoTermosController.renderizarDocumento(modelo, politica);
            assertTrue(html.contains("onclick=\"voltar()\""));
            assertTrue(html.contains("class=\"navegacao\""));
            assertTrue(html.contains("<style>"));
            assertTrue(html.contains("<h2>Objeto</h2>"));
            assertTrue(html.contains("Texto vigente"));
            assertFalse(html.contains("&lt;!DOCTYPE"));
            assertFalse(html.contains("Titulo antigo"));
            assertEquals(1, html.split("<h1>", -1).length - 1);
        }
    }
}
