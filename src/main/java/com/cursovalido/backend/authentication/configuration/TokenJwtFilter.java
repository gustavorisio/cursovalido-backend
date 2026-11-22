package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.service.TokenJwtService;
import com.cursovalido.backend.authentication.service.UsuarioDetalhesService;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import com.cursovalido.backend.authentication.security.RespostaErroHttp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TokenJwtFilter extends OncePerRequestFilter {
    private final TokenJwtService tokens;
    private final UsuarioDetalhesService usuarios;

    private boolean podeAutenticar(String email) {
        if (email == null) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication() == null;
    }

    private boolean precisaReautenticar(UsuarioAutenticado usuario) {
        return !usuario.isTermosAceitos() || !usuario.isVersaoTermosAtiva();
    }

    private boolean rotaDeDecisao(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && "/api/autenticacao/decidir-termos".equals(request.getRequestURI());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        if ("POST".equals(request.getMethod())) {
            return switch (caminho) {
                case "/api/suporte/solicitacoes", "/api/autenticacao/login",
                        "/api/autenticacao/confirmar-2fa", "/api/autenticacao/reenviar-2fa" ->
                    true;
                default -> false;
            };
        }
        return "GET".equals(request.getMethod())
                && (caminho.equals("/api/politicas/ativas") || caminho.equals("/api/politicas/privacidade/ativa")
                        || caminho.equals("/api/politicas/historico")
                        || caminho.equals("/api/politicas/privacidade/historico")
                        || caminho.startsWith("/api/politicas/responsabilidade/")
                        || caminho.equals("/termos-aceite.html") || caminho.equals("/politica-privacidade.html"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadeia) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null) {
            cadeia.doFilter(request, response);
            return;
        }
        if (cabecalho.startsWith("Bearer ")) {
            try {
                String token = cabecalho.substring(7);
                var dadosToken = tokens.dadosParaRequisicao(token);
                String email = dadosToken.email();
                if (podeAutenticar(email)) {
                    var detalhes = usuarios.loadUserForRequest(email, dadosToken.idUsuario(), dadosToken.idSessao());
                    if (!detalhes.getId().equals(dadosToken.idUsuario())) {
                        RespostaErroHttp.escrever(response, 401,
                                "Seu acesso não é mais válido. Entre na sua conta novamente.", "SESSAO_INVALIDA");
                        return;
                    }
                    boolean root = detalhes.isAdministradorPrincipal();
                    if (!detalhes.isEnabled()) {
                        RespostaErroHttp.escrever(response, 403,
                                "Sua conta não tem acesso no momento. Entre em contato pelo Suporte.", "ACESSO_NEGADO");
                        return;
                    }
                    boolean termos = dadosToken.apenasTermos();
                    String caminho = request.getRequestURI();
                    boolean decidir = rotaDeDecisao(request);
                    boolean logout = "POST".equals(request.getMethod()) && caminho.equals("/api/autenticacao/logout");
                    boolean confirmar = "POST".equals(request.getMethod())
                            && caminho.equals("/api/autenticacao/aceitar-termos-novamente");
                    boolean reautenticar = !root && !termos && precisaReautenticar(detalhes);
                    if (reautenticar && !decidir && !logout) {
                        RespostaErroHttp.escrever(response, 403,
                                "Leia os termos vigentes e entre novamente para confirmar sua decisao.",
                                "TERMOS_REAUTENTICACAO");
                        return;
                    }
                    if (!root && termos && !decidir && !logout && !confirmar) {
                        RespostaErroHttp.escrever(response, 403, "Acesso restrito aos termos.", "ACESSO_NEGADO");
                        return;
                    }
                    var autoridades = detalhes.getAuthorities();
                    if (root) {
                        autoridades = List.of(() -> "ROLE_ADMINISTRADOR");
                    } else if (termos) {
                        autoridades = List.of(() -> "TERMOS_PENDENTES");
                    } else if (reautenticar) {
                        autoridades = List.of(() -> "TERMOS_REAUTENTICACAO");
                    }
                    var autenticacao = new UsernamePasswordAuthenticationToken(detalhes, null, autoridades);
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            } catch (Exception excecao) {
                SecurityContextHolder.clearContext();
            }
        }
        cadeia.doFilter(request, response);
    }
}
