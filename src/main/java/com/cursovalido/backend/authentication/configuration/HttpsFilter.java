package com.cursovalido.backend.authentication.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import com.cursovalido.backend.authentication.security.RespostaErroHttp;

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
            RespostaErroHttp.escrever(response, 426, "Não foi possível abrir uma conexão segura. Atualize a página e tente novamente.", "HTTPS_OBRIGATORIO");
            return;
        }
        cadeia.doFilter(request, response);
    }
}
