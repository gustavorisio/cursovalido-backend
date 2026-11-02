package com.cursovalido.backend.aulas.service;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.cursovalido.backend.aulas.service.AulasTestSupport.*;

// Exerce a fabrica HTTP e o timeout usados em producao, contra um servidor local.
class YoutubeClientHttpTest {
    private HttpServer servidor;

    @AfterEach
    void encerrar() {
        if (servidor != null) { servidor.stop(0); }
    }

    @Test
    void clienteRealEnviaParametrosEChaveNoCabecalho() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> parametros = new AtomicReference<>();
        AtomicReference<String> chave = new AtomicReference<>();
        servidor.createContext("/youtube/v3/videos", troca -> {
            parametros.set(troca.getRequestURI().getRawQuery());
            chave.set(troca.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            byte[] corpo = """
                    {"items":[{"id":"M7lc1UVf-VE","status":{"embeddable":true,
                    "privacyStatus":"public","uploadStatus":"processed"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().set("Content-Type", "application/json");
            troca.sendResponseHeaders(200, corpo.length);
            try (var saida = troca.getResponseBody()) { saida.write(corpo); }
        });
        servidor.start();
        new YoutubeClient(url(), "chave-ficticia", 1000, 1000).validarVideo("M7lc1UVf-VE");
        assertThat(parametros.get()).isEqualTo("part=status&id=M7lc1UVf-VE");
        assertThat(parametros.get()).doesNotContain("chave-ficticia");
        assertThat(chave.get()).isEqualTo("chave-ficticia");
    }

    @Test
    void timeoutRealDeLeituraRetornaErroControlado() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch liberar = new CountDownLatch(1);
        servidor.createContext("/youtube/v3/videos", troca -> {
            try {
                liberar.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally { troca.close(); }
        });
        servidor.start();
        try {
            var cliente = new YoutubeClient(url(), "chave-ficticia", 1000, 250);
            erro(() -> cliente.validarVideo("M7lc1UVf-VE"), 503);
        } finally { liberar.countDown(); }
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/youtube/v3";
    }
}
