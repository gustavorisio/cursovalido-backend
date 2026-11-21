package com.cursovalido.backend.authentication.security;

public final class AnonimizacaoDados {
    private AnonimizacaoDados() {
    }

    public static String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank())
            return "Aluno";
        return nomeCompleto.strip().split("\\s+")[0];
    }

    public static String abreviarNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank())
            return "Aluno";
        String[] partes = nomeCompleto.strip().split("\\s+");
        if (partes.length == 1)
            return partes[0];
        String ultimo = partes[partes.length - 1];
        return partes[0] + " " + ultimo.substring(0, ultimo.offsetByCodePoints(0, 1)).toUpperCase() + ".";
    }
}
