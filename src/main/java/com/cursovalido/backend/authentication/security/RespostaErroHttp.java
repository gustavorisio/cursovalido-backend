package com.cursovalido.backend.authentication.security;

import com.cursovalido.backend.authentication.dto.Codigo2faErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class RespostaErroHttp {
    private static final ObjectMapper JSON = new ObjectMapper();

    private RespostaErroHttp() {
    }

    public static void escrever(HttpServletResponse resposta, int status, String mensagem, String codigo)
            throws IOException {
        resposta.setStatus(status);
        resposta.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(resposta.getWriter(), new Codigo2faErroResponse(mensagem, codigo));
    }
}
