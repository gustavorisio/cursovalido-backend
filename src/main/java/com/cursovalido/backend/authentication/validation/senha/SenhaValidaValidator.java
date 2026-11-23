package com.cursovalido.backend.authentication.validation.senha;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        // Delega a validação de campo vazio ao @NotBlank.
        if (valor == null || valor.isEmpty()) {
            return true;
        }
        var problema = RegrasSenha.primeiroProblema(valor);
        if (problema.isEmpty()) {
            return true;
        }
        // Informa a primeira regra de senha não atendida.
        if (contexto != null) {
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(problema.get()).addConstraintViolation();
        }
        return false;
    }
}
