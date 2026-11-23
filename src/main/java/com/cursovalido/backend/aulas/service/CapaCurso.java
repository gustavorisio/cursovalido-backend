package com.cursovalido.backend.aulas.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class CapaCurso {
    private static final String PREFIXO = "data:image/jpeg;base64,";
    private static final int LIMITE = 2 * 1024 * 1024;

    private CapaCurso() {
    }

    static void validar(String capa) {
        if (capa == null)
            return;
        if (!capa.startsWith(PREFIXO) || capa.length() > PREFIXO.length() + 4 * ((LIMITE + 2) / 3))
            throw invalida();
        try {
            byte[] bytes = Base64.getDecoder().decode(capa.substring(PREFIXO.length()));
            if (bytes.length > LIMITE || bytes.length < 3 || (bytes[0] & 255) != 255
                    || (bytes[1] & 255) != 216 || (bytes[2] & 255) != 255)
                throw invalida();
            try (var entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var leitores = ImageIO.getImageReaders(entrada);
                if (!leitores.hasNext())
                    throw invalida();
                var leitor = leitores.next();
                try {
                    if (!"JPEG".equalsIgnoreCase(leitor.getFormatName()))
                        throw invalida();
                    leitor.setInput(entrada);
                    int largura = leitor.getWidth(0), altura = leitor.getHeight(0);
                    if (largura < 1 || altura < 1 || largura > 550 || altura > 250)
                        throw invalida();
                    if (leitor.read(0) == null)
                        throw invalida();
                } finally {
                    leitor.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw invalida();
        }
    }

    private static ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A capa deve ser um JPEG de ate 2 MB, com no maximo 550 pixels de largura e 250 de altura.");
    }
}
