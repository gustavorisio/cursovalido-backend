package com.cursovalido.backend.authentication.suporte;

import jakarta.validation.constraints.*;

public record SolicitacaoSuporteRequest(
        @NotBlank @Email @Size(max = 254) String emailConta,
        @NotBlank @Size(max = 100) String usuario,
        @NotBlank @Email @Size(max = 254) String emailContato,
        @NotNull Motivo motivo,
        @NotBlank @Size(max = 150) String assunto,
        @NotBlank @Size(min = 20, max = 5000) String descricao) {
    public enum Motivo {
        REVISAO_BANIMENTO, RECUPERACAO_CONTA, PROBLEMA_ACESSO, ERRO_SISTEMA, PRIVACIDADE, OUTROS
    }
}
