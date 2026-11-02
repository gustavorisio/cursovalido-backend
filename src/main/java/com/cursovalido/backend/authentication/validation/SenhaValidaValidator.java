package com.cursovalido.backend.authentication.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
