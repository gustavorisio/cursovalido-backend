package com.cursovalido.backend.authentication.controller;

import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.PrivacidadeService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoTermosRenderizacaoTest {
    @Test
    void deveExibirPoliticaDePrivacidadePublicada() throws Exception {
        var privacidade = mock(PrivacidadeService.class);
        when(privacidade.buscarAtiva()).thenReturn(new PoliticaResponse(
                "Privacidade", "2.0", "Texto vigente", LocalDateTime.of(2025, 1, 1, 12, 0)));
        var controller = new DocumentoTermosController(mock(PoliticaService.class), privacidade);

        var resposta = controller.documentoPrivacidade();

        assertEquals("public, max-age=60", resposta.getHeaders().getCacheControl());
        assertTrue(resposta.getBody().contains("Texto vigente"));
    }

    @Test
    void renderizaCorpoSemExibirCabecalhoHtml() {
        String texto = DocumentoTermosController.renderizarConteudo(
                "<!DOCTYPE html><html><head><title>Termos</title></head><body><h2>Objeto</h2><p>Texto <strong>importante</strong></p></body></html>");
        assertEquals("Objeto\nTexto importante", texto);
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
            try (var entrada = new ClassPathResource("static/" + recurso)
                    .getInputStream()) {
                modelo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }
            var politica = new PoliticaResponse("Documento", "1.0",
                    "<!DOCTYPE html><html><body><h1>Titulo antigo</h1><p><strong>Versao 1.0</strong></p><h2>Objeto</h2><p>Texto vigente</p></body></html>",
                    LocalDateTime.now());
            String html = DocumentoTermosController.renderizarDocumento(modelo, politica);
            assertTrue(html.contains("onclick=\"voltar()\""));
            assertTrue(html.contains("class=\"navegacao\""));
            assertTrue(html.contains("<style>"));
            assertTrue(html.contains("Objeto\nTexto vigente"));
            assertFalse(html.contains("<h2>"));
            assertFalse(html.contains("&lt;!DOCTYPE"));
            assertFalse(html.contains("Titulo antigo"));
            assertEquals(1, html.split("<h1>", -1).length - 1);
        }
    }
}
