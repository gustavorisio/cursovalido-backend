package com.cursovalido.backend.authentication.validation.responsabilidade;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.service.ResponsabilidadeService;

public final class ResponsabilidadeValidation {
    private ResponsabilidadeValidation() {
    }

    public static void validar(Perfil perfil, Boolean aceitou, String versao, String ip,
            ResponsabilidadeService responsabilidade) {
        if (perfil == Perfil.ALUNO) return;
        if (!Boolean.TRUE.equals(aceitou) || !responsabilidade.documento(perfil).versao().equals(versao))
            throw new IllegalArgumentException("Aceite da versao vigente do termo de responsabilidade obrigatorio");
        if (ip == null || ip.isBlank() || ip.length() > 45)
            throw new IllegalArgumentException("Endereco IP do aceite obrigatorio");
    }
}
