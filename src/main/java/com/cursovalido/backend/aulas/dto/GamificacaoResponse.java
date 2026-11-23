package com.cursovalido.backend.aulas.dto;

public record GamificacaoResponse(int nivel, long xpNivel, long metaNivel, long xpParaProximoNivel,
                int percentualNivel, long xpTotal, long cursosConcluidos, long questionariosConcluidos,
                boolean nivelMaximo) {
}
