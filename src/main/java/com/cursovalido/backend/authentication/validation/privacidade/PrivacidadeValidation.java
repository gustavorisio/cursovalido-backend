package com.cursovalido.backend.authentication.validation.privacidade;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.repository.PrivacidadeRepository;

public final class PrivacidadeValidation {
    private PrivacidadeValidation() {
    }

    public static void validarPublicacao(PoliticaRequest dados, PrivacidadeRepository repositorio) {
        if (repositorio.findByVersao(dados.versao().trim()).isPresent())
            throw new IllegalArgumentException("A versao informada ja existe");
    }
}
