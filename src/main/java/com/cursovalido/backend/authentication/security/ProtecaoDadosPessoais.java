package com.cursovalido.backend.authentication.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ProtecaoDadosPessoais {
    private ProtecaoDadosPessoais() {
    }

    public static String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static String hashCpf(String cpf) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalizarCpf(cpf).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception erro) {
            throw new IllegalStateException("Nao foi possivel proteger o CPF", erro);
        }
    }

    public static String mascararCpf(String cpf) {
        String normalizado = normalizarCpf(cpf);
        if (normalizado == null || normalizado.length() != 11) {
            return "***.***.***-**";
        }
        return "***.***.***-" + normalizado.substring(9);
    }
}