package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record SuspensaoRequest(@NotNull EscopoSuspensao escopo,
        @NotBlank @Size(max = 1000) String motivo, @Future Instant expiraEm) {
}
