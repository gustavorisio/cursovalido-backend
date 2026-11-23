package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.RespostaErroHttp;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class LimiteRequisicoesFilter extends OncePerRequestFilter {
    private static final Set<String> ALTERACOES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Map<String, Integer> INTERVALOS_CONTA = Map.ofEntries(
            Map.entry("GET /api/autenticacao/exportar-dados", 10),
            Map.entry("PUT /api/autenticacao/segundo-fator", 30),
            Map.entry("POST /api/autenticacao/solicitar-alteracao-senha", 10),
            Map.entry("POST /api/autenticacao/alterar-senha", 10),
            Map.entry("POST /api/autenticacao/alterar-senha-autenticado", 10),
            Map.entry("POST /api/autenticacao/conta/solicitar-exclusao", 10),
            Map.entry("DELETE /api/autenticacao/conta", 10));
    private static final int CAPACIDADE = 10_000;
    private final Map<String, Long> proximas = new LinkedHashMap<>();

    private static final Set<String> ROTAS_AUTENTICACAO_RESTRITAS = Set.of(
            "/api/autenticacao/login",
            "/api/autenticacao/cadastro",
            "/api/autenticacao/confirmar-2fa",
            "/api/autenticacao/reenviar-2fa",
            "/api/autenticacao/solicitar-alteracao-senha",
            "/api/autenticacao/alterar-senha",
            "/api/autenticacao/alterar-senha-autenticado");
    private final Map<String, Janela> janelas = new LinkedHashMap<>(128, 0.75f, true);
    private final Clock relogio;
    private final int limite;
    private final int limiteAutenticacao;

    public LimiteRequisicoesFilter() {
        this(30, 5);
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
        int segundos = intervalo(request, caminho);
        int aguardar;
        synchronized (this) {
            long agora = relogio.millis();
            String chave = chave(request, caminho);
            Long prazo = proximas.get(chave);
            aguardar = prazo == null ? 0 : (int) Math.max(0, (prazo - agora + 999) / 1000);
            if (aguardar == 0 && deveLimitar(request)) {
                int quota = ROTAS_AUTENTICACAO_RESTRITAS.contains(caminho) ? limiteAutenticacao : limite;
                aguardar = aguardar(request.getRemoteAddr(), grupo(request, caminho), quota, relogio.instant());
            }
            if (aguardar == 0 && segundos > 0) {
                proximas.entrySet().removeIf(item -> item.getValue() <= agora);
                if (proximas.size() >= CAPACIDADE)
                    aguardar = segundos;
                else {
                    proximas.put(chave, agora + segundos * 1000L);
                    response.setHeader("X-Intervalo-Acao", Integer.toString(segundos));
                }
            }
        }
        if (aguardar > 0) {
            response.setHeader("Retry-After", Integer.toString(aguardar));
            RespostaErroHttp.escrever(response, 429,
                    "Aguarde " + aguardar + " segundos antes de repetir esta ação.", "LIMITE_REQUISICOES_ATINGIDO");
            return;
        }
        cadeia.doFilter(request, response);
    }

    private int intervalo(HttpServletRequest request, String caminho) {
        String metodo = "HEAD".equals(request.getMethod()) ? "GET" : request.getMethod();
        Integer intervaloConta = INTERVALOS_CONTA.get(metodo + " " + caminho);
        if (intervaloConta != null)
            return intervaloConta;
        // Usa o prazo salvo no banco para estas ações.
        if (("POST".equals(metodo) && (caminho.equals("/api/administracao/comunicados")
                || caminho.equals("/api/administracao/convites")))
                || caminho.matches("^/api/administracao/convites/[^/]+/reenviar$"))
            return 0;
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        boolean administrador = autenticacao != null
                && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario
                && usuario.getPerfil() == Perfil.ADMINISTRADOR;
        boolean moderacao = caminho.matches("^/api/forum/topicos/[^/]+/(fechar|arquivar)$")
                || ("DELETE".equals(request.getMethod())
                        && caminho.matches("^/api/forum/(comentarios/[^/]+|topicos/[^/]+/comentarios/[^/]+)$"));
        boolean administrativa = caminho.matches("^/api/(administracao|politicas)(/.*)?$")
                || (administrador && (caminho.matches("^/api/temas(/.*)?$") || moderacao));
        return ALTERACOES.contains(metodo) && administrativa ? 10 : 0;
    }

    private String chave(HttpServletRequest request, String caminho) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        String origem = autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario
                ? "usuario:" + usuario.getId()
                : "ip:" + request.getRemoteAddr();
        return origem + " " + ("HEAD".equals(request.getMethod()) ? "GET" : request.getMethod()) + " " + caminho;
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
                || ("GET".equals(metodo) && documento.startsWith("/api/forum/"))
                || (("GET".equals(metodo) || "HEAD".equals(metodo))
                        && documento.equals("/api/autenticacao/exportar-dados"));
    }

    private static String caminho(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    // Agrupa as rotas em contadores fixos.
    private static String grupo(HttpServletRequest request, String caminho) {
        if (ROTAS_AUTENTICACAO_RESTRITAS.contains(caminho))
            return caminho;
        if (caminho.equals("/api/autenticacao/exportar-dados"))
            return "EXPORTACAO_DADOS";
        if (caminho.startsWith("/api/forum/"))
            return "GET".equals(request.getMethod()) ? "FORUM_LEITURA" : "FORUM_ESCRITA";
        return caminho.startsWith("/api/") ? "API_ESCRITA" : "DOCUMENTOS";
    }

    private int aguardar(String ip, String grupo, int limiteDoGrupo, Instant agora) {
        Janela janela = janelas.get(ip);
        if (janela == null || !agora.isBefore(janela.inicio.plusSeconds(60))) {
            if (janela == null && janelas.size() >= CAPACIDADE) {
                // Substitui a origem menos recente quando o mapa está cheio.
                janelas.remove(janelas.keySet().iterator().next());
            }
            janela = new Janela(agora);
            janelas.put(ip, janela);
        }
        int usadas = janela.grupos.getOrDefault(grupo, 0);
        if (janela.quantidade >= limite || usadas >= limiteDoGrupo) {
            long milissegundos = Duration.between(agora, janela.inicio.plusSeconds(60)).toMillis();
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
