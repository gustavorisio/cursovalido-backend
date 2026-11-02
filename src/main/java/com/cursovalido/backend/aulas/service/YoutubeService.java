package com.cursovalido.backend.aulas.service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class YoutubeService {
    private final YoutubeClient cliente;

    public String validarVideo(String url) {
        String videoId = extrairVideoId(url);
        cliente.validarVideo(videoId);
        return videoId;
    }

    // Valida o link e guarda somente o ID usado pelo player do YouTube.
    public String extrairVideoId(String url) {
        if (url == null) {
            throw linkInvalido();
        }
        if (url.isBlank()) {
            throw linkInvalido();
        }
        if (url.length() > 2048) {
            throw linkInvalido();
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw linkInvalido();
            }
            if (host == null) {
                throw linkInvalido();
            }
            if (uri.getUserInfo() != null) {
                throw linkInvalido();
            }
            if (uri.getPort() != -1) {
                throw linkInvalido();
            }
            host = host.toLowerCase(Locale.ROOT);
            String id = null;
            if ("youtu.be".equals(host)) {
                id = uri.getPath().substring(1);
            } else {
                verificarHostYoutube(host);
                id = extrairIdDaConsulta(uri);
            }
            if (id == null) {
                throw linkInvalido();
            }
            if (!id.matches("[A-Za-z0-9_-]{11}")) {
                throw linkInvalido();
            }
            return id;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw linkInvalido();
        }
    }

    private void verificarHostYoutube(String host) {
        if ("youtube.com".equals(host)) {
            return;
        }
        if ("www.youtube.com".equals(host)) {
            return;
        }
        throw linkInvalido();
    }

    private String extrairIdDaConsulta(URI uri) {
        if (!"/watch".equals(uri.getPath())) {
            throw linkInvalido();
        }
        if (uri.getRawQuery() == null) {
            throw linkInvalido();
        }
        String id = null;
        // O caractere abaixo separa os parametros da URL; nao e um operador logico.
        for (String parametro : uri.getRawQuery().split("&")) {
            String[] partes = parametro.split("=", 2);
            if (!"v".equals(partes[0])) {
                continue;
            }
            if (partes.length != 2) {
                continue;
            }
            if (id != null) {
                throw linkInvalido();
            }
            id = URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
        }
        return id;
    }

    private ResponseStatusException linkInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe um link HTTPS do YouTube no formato youtube.com/watch?v=ID ou youtu.be/ID");
    }
}
