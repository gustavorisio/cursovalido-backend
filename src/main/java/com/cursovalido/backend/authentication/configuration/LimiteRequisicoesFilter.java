package com.cursovalido.backend.authentication.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;
import com.cursovalido.backend.authentication.security.RespostaErroHttp;

public class LimiteRequisicoesFilter extends OncePerRequestFilter {
    private static final Set<String> ROTAS_AUTENTICACAO_RESTRITAS = Set.of(
            "/api/autenticacao/login",
            "/api/autenticacao/cadastro",
            "/api/autenticacao/confirmar-2fa",
            "/api/autenticacao/reenviar-2fa",
            "/api/autenticacao/solicitar-alteracao-senha",
            "/api/autenticacao/alterar-senha",
            "/api/autenticacao/alterar-senha-autenticado");
    private final Map<String, Janela> janelas = new HashMap<>();
    private final int limite;

    public LimiteRequisicoesFilter() {
        this(30);
    }

    public LimiteRequisicoesFilter(int limite) {
        if (limite < 1)
            throw new IllegalArgumentException("O limite de solicitacoes deve ser positivo");
        this.limite = limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
            throws ServletException, IOException {
        String caminho = request.getRequestURI();
        if (deveLimitar(request)) {
            int limiteDaRota = ROTAS_AUTENTICACAO_RESTRITAS.contains(caminho) ? 5 : limite;
            int aguardar = 60;
            if (!permitir(request.getRemoteAddr() + ":" + caminho, limiteDaRota, Instant.now())) {
                response.setHeader("Retry-After", String.valueOf(aguardar));
                RespostaErroHttp.escrever(response, 429, "Muitas solicitações em sequência. Aguarde um minuto e tente novamente.",
                        "LIMITE_REQUISICOES_ATINGIDO");
                return;
            }
        }
        cadeia.doFilter(request, response);
    }

    private boolean deveLimitar(HttpServletRequest request) {
        String documento = request.getRequestURI();
        if ("GET".equals(request.getMethod()) && ("/termos-aceite.html".equals(documento)
                || "/politica-privacidade.html".equals(documento)))
            return true;
        String metodo = request.getMethod();
        boolean alteracao = "POST".equals(metodo) || "PUT".equals(metodo)
                || "PATCH".equals(metodo) || "DELETE".equals(metodo);
        return alteracao && request.getRequestURI().startsWith("/api/");
    }

    private synchronized boolean permitir(String identificador, int limiteDaRota, Instant agora) {
        janelas.entrySet().removeIf(item -> item.getValue().inicio.plusSeconds(60).isBefore(agora));
        Janela janela = janelas.get(identificador);
        if (janela == null) {
            if (janelas.size() >= 10_000)
                return false;
            janela = new Janela();
            janelas.put(identificador, janela);
        }
        janela.quantidade++;
        return janela.quantidade <= limiteDaRota;
    }

    private static class Janela {
        private final Instant inicio = Instant.now();
        private int quantidade;
    }
}
