package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.service.TokenJwtServico;
import com.cursovalido.backend.authentication.service.UsuarioDetalhesServico;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
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
public class TokenJwtFiltro extends OncePerRequestFilter {
    private final TokenJwtServico tokens;
    private final UsuarioDetalhesServico usuarios;
    private final com.cursovalido.backend.authentication.repository.UsuarioRepositorio repositorio;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain cadeia) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            try {
                String token = cabecalho.substring(7);
                String email = tokens.extrairEmail(token);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var detalhes = usuarios.loadUserByUsername(email);
                    if (detalhes instanceof UsuarioAutenticado usuario) {
                        if (!usuario.getId().equals(tokens.extrairIdUsuario(token))) {
                            response.sendError(401, "Sessao de outra conta");
                            return;
                        }
                    }
                    if (!detalhes.isEnabled()) {
                        response.sendError(403, "Conta sem permissao de acesso");
                        return;
                    }
                    boolean termos = tokens.apenasTermos(token);
                    if (!termos && detalhes instanceof UsuarioAutenticado usuario && !usuario.isTermosAceitos()
                            && !request.getRequestURI().equals("/api/autenticacao/logout")) {
                        var conta = repositorio.findById(usuario.getId()).orElseThrow();
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                                new com.cursovalido.backend.authentication.dto.RespostaTermosPendentes(
                                        "É necessário aceitar a nova versão dos termos.", "TERMOS_NAO_ACEITOS",
                                        tokens.gerarTermos(conta))));
                        return;
                    }
                    if (termos && !request.getRequestURI().equals("/api/autenticacao/aceitar-termos-novamente")
                            && !request.getRequestURI().equals("/api/autenticacao/logout")) {
                        response.sendError(403, "Token permitido apenas para aceite dos termos");
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
