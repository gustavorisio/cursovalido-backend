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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class TokenJwtFilter extends OncePerRequestFilter {
    private final TokenJwtService tokens;
    private final UsuarioDetalhesService usuarios;
    private final com.cursovalido.backend.authentication.repository.UsuarioRepository repositorio;

    private boolean podeAutenticar(String email) {
        if (email == null) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication() == null;
    }

    private boolean precisaAceitarTermos(boolean tokenDeTermos,
            org.springframework.security.core.userdetails.UserDetails detalhes, String caminho) {
        if (tokenDeTermos) {
            return false;
        }
        if (!(detalhes instanceof UsuarioAutenticado usuario)) {
            return false;
        }
        if (usuario.isTermosAceitos()) {
            return false;
        }
        return !caminho.equals("/api/autenticacao/logout");
    }

    private boolean rotaBloqueadaParaTokenDeTermos(boolean tokenDeTermos, String caminho) {
        if (!tokenDeTermos) {
            return false;
        }
        if (caminho.equals("/api/autenticacao/aceitar-termos-novamente")) {
            return false;
        }
        return !caminho.equals("/api/autenticacao/logout");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && "/api/suporte/solicitacoes".equals(request.getRequestURI());
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
                String email = tokens.extrairEmail(token);
                if (podeAutenticar(email)) {
                    var detalhes = usuarios.loadUserByUsername(email);
                    if (detalhes instanceof UsuarioAutenticado usuario) {
                        if (!usuario.getId().equals(tokens.extrairIdUsuario(token))) {
                            RespostaErroHttp.escrever(response, 401, "Seu acesso não é mais válido. Entre na sua conta novamente.", "SESSAO_INVALIDA");
                            return;
                        }
                    }
                    if (!detalhes.isEnabled()) {
                        RespostaErroHttp.escrever(response, 403, "Sua conta não tem acesso no momento. Entre em contato pelo Suporte.", "ACESSO_NEGADO");
                        return;
                    }
                    boolean termos = tokens.apenasTermos(token);
                    if (precisaAceitarTermos(termos, detalhes, request.getRequestURI())) {
                        UsuarioAutenticado usuario = (UsuarioAutenticado) detalhes;
                        var conta = repositorio.findById(usuario.getId()).orElseThrow();
                        response.setStatus(403);
                        response.setHeader("Cache-Control", "no-store");
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                                new com.cursovalido.backend.authentication.dto.TermosPendentesResponse(
                                        "Você precisa aceitar os Termos de Uso e a Política de Privacidade para continuar.", "TERMOS_NAO_ACEITOS",
                                        tokens.gerarTermos(conta))));
                        return;
                    }
                    if (rotaBloqueadaParaTokenDeTermos(termos, request.getRequestURI())) {
                        RespostaErroHttp.escrever(response, 403, "Você não tem permissão para fazer isso.", "ACESSO_NEGADO");
                        return;
                    }
                    var autenticacao = new UsernamePasswordAuthenticationToken(detalhes, null,
                            termos ? List.of(() -> "TERMOS_PENDENTES") : detalhes.getAuthorities());
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
