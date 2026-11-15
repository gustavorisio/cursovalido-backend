package com.cursovalido.backend.authentication.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.dto.PoliticaResponse;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.support.PostgresTeste;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VersionamentoDocumentosTest {
    @Autowired PoliticaService termos;
    @Autowired PrivacidadeService privacidade;
    @Autowired PoliticaRepository politicas;
    @Autowired PrivacidadeRepository privacidades;
    @Autowired VersaoDocumentoRepository versoes;
    @MockitoBean PrivilegiosAdministrativosService privilegios;
    @MockitoBean LogAuditoriaService auditoria;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        PostgresTeste.configurar(registry, "versionamento_documentos");
    }

    @BeforeEach
    void preparar() {
        politicas.deleteAll();
        privacidades.deleteAll();
        versoes.deleteAll();
        versoes.saveAllAndFlush(List.of(new VersaoDocumento("TERMOS", 0),
                new VersaoDocumento("PRIVACIDADE", 0)));
    }

    private PoliticaRequest documento(String conteudo) {
        return new PoliticaRequest("Documento", conteudo);
    }

    @Test
    void incrementaHistoricoSemAlterarConteudoAnteriorESeparaTipos() {
        assertEquals("1.0", termos.publicar(documento("Texto original"), 1L).versao());
        assertEquals("2.0", termos.publicar(documento("Texto revisado"), 1L).versao());
        assertEquals("1.0", privacidade.publicar(documento("Privacidade"), 1L).versao());
        var anterior = politicas.findByVersao("1.0").orElseThrow();
        assertEquals("Texto original", anterior.getConteudo());
        assertFalse(anterior.isAtiva());
        assertEquals(List.of("2.0"), termos.listarAtivas().stream().map(PoliticaResponse::versao).toList());
        assertEquals(2, versoes.findById("TERMOS").orElseThrow().getNumero());
    }

    @Test
    void falhaNaPublicacaoReverteContadorDocumentoEAtivacao() {
        termos.publicar(documento("Original"), 1L);
        doThrow(new IllegalStateException("Falha de auditoria"))
                .when(auditoria).registrar(1L, "POLITICA_PUBLICADA", "2.0", null);
        assertThrows(IllegalStateException.class, () -> termos.publicar(documento("Revisao"), 1L));
        assertEquals(1, versoes.findById("TERMOS").orElseThrow().getNumero());
        assertTrue(politicas.findByVersao("2.0").isEmpty());
        assertTrue(politicas.findByVersao("1.0").orElseThrow().isAtiva());
        reset(auditoria);
        assertEquals("2.0", termos.publicar(documento("Revisao"), 1L).versao());
    }

    @Test
    void publicacoesConcorrentesGeramVersoesUnicasEUmaUnicaAtiva() throws Exception {
        var prontas = new CountDownLatch(4);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<String>> resultados = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final int numero = i;
                resultados.add(executor.submit(() -> {
                    prontas.countDown();
                    assertTrue(iniciar.await(10, TimeUnit.SECONDS));
                    return termos.publicar(documento("Texto " + numero), 1L).versao();
                }));
            }
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            iniciar.countDown();
            Set<String> geradas = new HashSet<>();
            for (var resultado : resultados) geradas.add(resultado.get(20, TimeUnit.SECONDS));
            assertEquals(Set.of("1.0", "2.0", "3.0", "4.0"), geradas);
        }
        assertEquals(4, politicas.count());
        assertEquals(List.of("4.0"), termos.listarAtivas().stream().map(PoliticaResponse::versao).toList());
    }

    @Test
    void usaContadorPersistidoENaoEstadoDaInstancia() {
        versoes.saveAndFlush(new VersaoDocumento("TERMOS", 12));
        assertEquals("13.0", termos.publicar(documento("Persistencia"), 1L).versao());
    }

    @Test
    void novaPublicacaoNaoReutilizaCabecalhoComVersaoManualLegada() {
        var publicado = termos.publicar(documento("<h1>Termo antigo</h1>"
                + "<p><strong>Versão 99.0</strong></p><h2>Regras</h2><p>Texto revisado</p>"), 1L);
        assertEquals("1.0", publicado.versao());
        assertEquals("<h2>Regras</h2><p>Texto revisado</p>", publicado.conteudo());
    }

    @Test
    void acessoNegadoNaoConsomeVersao() {
        doThrow(new IllegalArgumentException("Sem permissao")).when(privilegios).verificarPrincipal(2L);
        assertThrows(IllegalArgumentException.class, () -> termos.publicar(documento("Texto"), 2L));
        assertEquals(0, versoes.findById("TERMOS").orElseThrow().getNumero());
        assertEquals(0, politicas.count());
    }
}
