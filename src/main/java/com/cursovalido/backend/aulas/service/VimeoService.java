package com.cursovalido.backend.aulas.service;

import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Valida a URL e confirma o proprietário do vídeo antes da gravação. */
@Service
@RequiredArgsConstructor
public class VimeoService {
    private final VimeoClient cliente;

    public String validarVideo(String valor) {
        String url = validarUrl(valor);
        if (url != null)
            cliente.validarVideo(extrairVideoId(url));
        return url;
    }

    private String extrairVideoId(String url) {
        URI uri = URI.create(url);
        var partes = uri.getRawPath().split("/");
        return uri.getHost().equalsIgnoreCase("player.vimeo.com") ? partes[2] : partes[1];
    }

    private static String validarUrl(String valor) {
        if (valor == null || valor.isBlank())
            return null;
        try {
            URI uri = URI.create(valor.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean hostPermitido = host.equals("vimeo.com") || host.equals("www.vimeo.com")
                    || host.equals("player.vimeo.com");
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !hostPermitido || uri.getUserInfo() != null
                    || uri.getPort() != -1)
                throw new IllegalArgumentException();
            if (valor.length() > 2048)
                throw new IllegalArgumentException();
            String formato = host.equals("player.vimeo.com")
                    ? "^/video/[0-9]{1,20}/?$"
                    : "^/[0-9]{1,20}(?:/[a-zA-Z0-9]{1,64})?/?$";
            if (!uri.getRawPath().matches(formato))
                throw new IllegalArgumentException();
            return uri.toString();
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma URL HTTPS válida de vídeo do Vimeo.");
        }
    }
}
