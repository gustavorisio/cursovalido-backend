package com.cursovalido.backend.aulas.service;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validação local das URLs públicas/privadas do Vimeo usadas pelo player incorporado. */
public final class VimeoService {
    private static final Pattern ID = Pattern.compile("[0-9]{1,20}");

    private VimeoService() {}

    public static String validarUrl(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            URI uri = URI.create(valor.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean hostPermitido = host.equals("vimeo.com") || host.equals("www.vimeo.com")
                    || host.equals("player.vimeo.com");
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !hostPermitido || uri.getUserInfo() != null
                    || uri.getPort() != -1) throw new IllegalArgumentException();
            String[] partes = uri.getPath().split("/");
            String id = null;
            if (host.equals("player.vimeo.com") && partes.length >= 3 && "video".equals(partes[1])) id = partes[2];
            else if (partes.length >= 2) {
                for (int i = partes.length - 1; i >= 1; i--) {
                    if (ID.matcher(partes[i]).matches()) { id = partes[i]; break; }
                }
            }
            if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException();
            return uri.toString();
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma URL HTTPS válida de vídeo do Vimeo.");
        }
    }
}
