package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenEmailRequest(
        @NotBlank(message = "O link é inválido. Abra novamente o link recebido por e-mail.") @Size(max = 43, message = "O link é inválido. Abra novamente o link recebido por e-mail.") String token) {
}
