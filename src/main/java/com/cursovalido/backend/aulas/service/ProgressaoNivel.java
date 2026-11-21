package com.cursovalido.backend.aulas.service;

public final class ProgressaoNivel {
    public static final int NIVEL_MAXIMO = 100;
    public static final long META_INICIAL = 100;
    public static final double FATOR_CRESCIMENTO = 1.5;

    private ProgressaoNivel() {
    }

    // Meta do nivel: 100, 150, 230, 340, 510... cada nivel exige mais XP que o anterior.
    public static long meta(int nivel) {
        int atual = Math.max(1, Math.min(nivel, NIVEL_MAXIMO));
        double bruto = META_INICIAL * Math.pow(FATOR_CRESCIMENTO, atual - 1);
        long arredondado = Math.round(bruto / 10.0) * 10;
        return Math.max(META_INICIAL, arredondado);
    }

    public static Resultado aplicar(int nivel, long xpNivel, long ganho) {
        int novoNivel = Math.max(1, nivel);
        long saldo = Math.max(0, xpNivel) + Math.max(0, ganho);
        while (novoNivel < NIVEL_MAXIMO && saldo >= meta(novoNivel)) {
            saldo -= meta(novoNivel);
            novoNivel++;
        }
        return new Resultado(novoNivel, saldo);
    }

    public record Resultado(int nivel, long xpNivel) {
    }
}
