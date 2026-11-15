package com.cursovalido.backend.authentication.validation.termos;

import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.service.PoliticaService;

public final class TermosValidation {
    private TermosValidation() {
    }

    public static void validarVersao(String versaoTermos, PoliticaService politicas) {
        if (!politicas.versaoAtiva(versaoTermos))
            throw new TermosNaoDisponiveisException();
    }
}
