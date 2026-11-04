package com.cursovalido.backend.authentication.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;

public class LimiteRequisicoesFiltro extends OncePerRequestFilter {
    private final Map<String, Janela> janelas = new HashMap<>();
    private final int limite;

    public LimiteRequisicoesFiltro() {
        this(30);
    }

    public LimiteRequisicoesFiltro(int limite) {
        if (limite < 1)
            throw new IllegalArgumentException("O limite de solicitacoes deve ser positivo");
        this.limite = limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
            throws ServletException, IOException {
        String caminho = request.getRequestURI();
        if (("POST".equals(request.getMethod()) || "DELETE".equals(request.getMethod()))
                && (caminho.startsWith("/api/autenticacao/") || caminho.startsWith("/api/convites/"))) {
            if (!permitir(request.getRemoteAddr() + ":" + caminho)) {
                response.setHeader("Retry-After", "60");
                response.sendError(429, "Muitas solicitacoes. Aguarde um minuto.");
                return;
            }
        }
        cadeia.doFilter(request, response);
    }

    private synchronized boolean permitir(String identificador) {
        Instant agora = Instant.now();
        janelas.entrySet().removeIf(item -> item.getValue().inicio.plusSeconds(60).isBefore(agora));
        Janela janela = janelas.get(identificador);
        if (janela == null) {
            if (janelas.size() >= 10_000)
                return false;
            janela = new Janela();
            janelas.put(identificador, janela);
        }
        janela.quantidade++;
        return janela.quantidade <= limite;
    }

    private static class Janela {
        private final Instant inicio = Instant.now();
        private int quantidade;
    }
}
