package com.cursovalido.backend.authentication.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.time.Clock;
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
    private static final int MAXIMO_IPS = 10_000;
    private final Map<String, Janela> janelas = new LinkedHashMap<>(128, 0.75f, true);
    private final Clock relogio;
    private final int limite;
    private final int limiteAutenticacao;

    public LimiteRequisicoesFilter() {
        this(30, 30);
    }

    public LimiteRequisicoesFilter(int limite) {
        this(limite, limite);
    }

    public LimiteRequisicoesFilter(int limite, int limiteAutenticacao) {
        this(limite, limiteAutenticacao, Clock.systemUTC());
    }

    LimiteRequisicoesFilter(int limite, int limiteAutenticacao, Clock relogio) {
        if (limite < 1)
            throw new IllegalArgumentException("O limite de solicitacoes deve ser positivo");
        if (limiteAutenticacao < 1)
            throw new IllegalArgumentException("O limite de autenticacao deve ser positivo");
        this.relogio = relogio;
        this.limite = limite;
        this.limiteAutenticacao = limiteAutenticacao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
            throws ServletException, IOException {
        String caminho = caminho(request);
        if (deveLimitar(request)) {
            int limiteDaRota = ROTAS_AUTENTICACAO_RESTRITAS.contains(caminho) ? limiteAutenticacao : limite;
            int aguardar = aguardar(request.getRemoteAddr(), grupo(request, caminho), limiteDaRota, relogio.instant());
            if (aguardar > 0) {
                response.setHeader("Retry-After", String.valueOf(aguardar));
                RespostaErroHttp.escrever(response, 429, "Muitas solicitações em sequência. Aguarde um minuto e tente novamente.",
                        "LIMITE_REQUISICOES_ATINGIDO");
                return;
            }
        }
        cadeia.doFilter(request, response);
    }

    private boolean deveLimitar(HttpServletRequest request) {
        String documento = caminho(request);
        if ("GET".equals(request.getMethod()) && ("/termos-aceite.html".equals(documento)
                || "/politica-privacidade.html".equals(documento)))
            return true;
        String metodo = request.getMethod();
        boolean alteracao = "POST".equals(metodo) || "PUT".equals(metodo)
                || "PATCH".equals(metodo) || "DELETE".equals(metodo);
        return (alteracao && documento.startsWith("/api/"))
                || ("GET".equals(metodo) && documento.startsWith("/api/forum/"));
    }

    private static String caminho(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    // Somente grupos fixos: IDs e caminhos arbitrários nunca criam novos contadores.
    private static String grupo(HttpServletRequest request, String caminho) {
        if (ROTAS_AUTENTICACAO_RESTRITAS.contains(caminho)) return caminho;
        if (caminho.startsWith("/api/forum/"))
            return "GET".equals(request.getMethod()) ? "FORUM_LEITURA" : "FORUM_ESCRITA";
        return caminho.startsWith("/api/") ? "API_ESCRITA" : "DOCUMENTOS";
    }

    private synchronized int aguardar(String ip, String grupo, int limiteDoGrupo, Instant agora) {
        Janela janela = janelas.get(ip);
        if (janela == null || !agora.isBefore(janela.inicio.plusSeconds(60))) {
            if (janela == null && janelas.size() >= MAXIMO_IPS) {
                // Substitui a origem menos recente; capacidade cheia não bloqueia novos usuários.
                janelas.remove(janelas.keySet().iterator().next());
            }
            janela = new Janela(agora);
            janelas.put(ip, janela);
        }
        int usadas = janela.grupos.getOrDefault(grupo, 0);
        if (janela.quantidade >= limite || usadas >= limiteDoGrupo) {
            long milissegundos = java.time.Duration.between(agora, janela.inicio.plusSeconds(60)).toMillis();
            return (int) Math.max(1, (milissegundos + 999) / 1000);
        }
        janela.quantidade++;
        janela.grupos.put(grupo, usadas + 1);
        return 0;
    }

    private static class Janela {
        private final Instant inicio;
        private int quantidade;
        private final Map<String, Integer> grupos = new HashMap<>();

        private Janela(Instant inicio) {
            this.inicio = inicio;
        }
    }
}
