package com.cursovalido.backend.support;

import com.cursovalido.backend.authentication.configuration.LimiteRequisicoesFilter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

/** Avança apenas o relógio do limite HTTP, sem dormir nem alterar validade de tokens. */
public final class TempoLimitesTeste {
    private TempoLimitesTeste() {}

    public static void avancar(SecurityFilterChain cadeia) {
        cadeia.getFilters().stream().filter(LimiteRequisicoesFilter.class::isInstance).forEach(filtro -> {
            Clock atual = (Clock) ReflectionTestUtils.getField(filtro, "relogio");
            ReflectionTestUtils.setField(filtro, "relogio", Clock.offset(atual, Duration.ofMinutes(1)));
        });
    }
}
