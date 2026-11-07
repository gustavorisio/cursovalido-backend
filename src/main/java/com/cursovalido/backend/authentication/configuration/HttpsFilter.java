package com.cursovalido.backend.authentication.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class HttpsFilter extends OncePerRequestFilter {
    private final boolean obrigatorio;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
            throws ServletException, IOException {
        if (!obrigatorio) {
            cadeia.doFilter(request, response);
            return;
        }
        if (!request.isSecure()) {
            response.sendError(426, "Use HTTPS para acessar a API");
            return;
        }
        cadeia.doFilter(request, response);
    }
}
