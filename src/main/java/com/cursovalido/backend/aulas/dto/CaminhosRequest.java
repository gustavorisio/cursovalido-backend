package com.cursovalido.backend.aulas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CaminhosRequest(@NotBlank @Size(max = 255) String titulo,
        @NotNull @Size(max = 2000) String descricao, boolean publicado,
        @NotNull @Min(0) Integer versao, @Pattern(regexp = "[a-zA-Z0-9_-]{1,50}") String perguntaInicial,
        @NotNull @Size(max = 60) List<@NotNull @Valid Pergunta> perguntas,
        @NotNull @Size(max = 40) List<@NotNull @Valid Resultado> resultados) {
    public record Pergunta(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,50}") String chave,
            @NotBlank @Size(max = 1000) String enunciado,
            @NotNull @Size(min = 2, max = 8) List<@NotNull @Valid Opcao> opcoes) {}
    public record Opcao(@NotBlank @Size(max = 500) String texto,
            @Pattern(regexp = "[a-zA-Z0-9_-]{1,50}") String proximaPergunta,
            @Pattern(regexp = "[a-zA-Z0-9_-]{1,50}") String resultado) {}
    public record Resultado(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,50}") String chave,
            @NotBlank @Size(max = 255) String titulo, @NotBlank @Size(max = 5000) String orientacao,
            @NotNull @Size(min = 1, max = 15) List<@NotBlank @Size(max = 150) String> temasPesquisa) {}
}
