package com.cursovalido.backend.authentication.validation.responsabilidade;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.service.ResponsabilidadeService;

public final class ResponsabilidadeValidation {
    private ResponsabilidadeValidation() {
    }

    public static void validar(Perfil perfil, Boolean aceitou, String versao, String ip,
            ResponsabilidadeService responsabilidade) {
        if (perfil == null || !perfil.exigeTermoResponsabilidade())
            return;
        if (!Boolean.TRUE.equals(aceitou) || !responsabilidade.documento(perfil).versao().equals(versao))
            throw new IllegalArgumentException("Aceite a versão atual do Termo de Responsabilidade para continuar.");
        if (ip == null || ip.isBlank() || ip.length() > 45)
            throw new IllegalArgumentException("Não foi possível registrar o aceite. Tente novamente.");
    }
}
