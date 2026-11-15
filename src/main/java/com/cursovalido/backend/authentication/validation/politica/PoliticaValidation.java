package com.cursovalido.backend.authentication.validation.politica;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.repository.PoliticaRepository;

public final class PoliticaValidation {
    private PoliticaValidation() {
    }

    public static void validarPublicacao(PoliticaRequest requisicao, PoliticaRepository repositorio) {
        if (repositorio.findByVersao(requisicao.versao()).isPresent()) {
            throw new IllegalArgumentException("A versao informada ja existe");
        }
    }
}
