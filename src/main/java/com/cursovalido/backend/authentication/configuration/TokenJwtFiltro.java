package com.cursovalido.backend.authentication.configuration;

import com.cursovalido.backend.authentication.service.TokenJwtServico;
import com.cursovalido.backend.authentication.service.UsuarioDetalhesServico;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenJwtFiltro extends OncePerRequestFilter {
    @Autowired
    private TokenJwtServico tokens;
    @Autowired
    private UsuarioDetalhesServico usuarios;

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
                    var autenticacao = new UsernamePasswordAuthenticationToken(detalhes, null,
                            detalhes.getAuthorities());
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            } catch (Exception excecao) {
                SecurityContextHolder.clearContext();
                excecao.printStackTrace();
            }
        }
        cadeia.doFilter(request, response);
    }
}