package com.cursovalido.backend.authentication.validation.suporte;

import com.cursovalido.backend.authentication.exception.SuporteIndisponivelException;
import com.cursovalido.backend.authentication.suporte.LimiteSuporteException;
import java.time.Duration;
import java.time.Instant;

public final class SuporteValidation {
    private SuporteValidation() {
    }

    public static void validarLimite(Instant agora, Instant liberacao) {
        if (liberacao.isAfter(agora)) {
            throw new LimiteSuporteException(
                    Math.max(1, (Duration.between(agora, liberacao).toMillis() + 999) / 1000));
        }
    }

    public static void validarDisponibilidade(String destinatario, org.slf4j.Logger log) {
        if (destinatario.isBlank()) {
            log.warn("Suporte indisponivel: configure SUPPORT_EMAIL no ambiente do processo e reinicie o backend.");
            throw new SuporteIndisponivelException();
        }
    }
}
