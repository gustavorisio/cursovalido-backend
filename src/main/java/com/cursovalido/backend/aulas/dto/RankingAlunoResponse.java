package com.cursovalido.backend.aulas.dto;

public record RankingAlunoResponse(int posicao, String nome, int nivel, long comentarios, long aulasConcluidas) {
}
