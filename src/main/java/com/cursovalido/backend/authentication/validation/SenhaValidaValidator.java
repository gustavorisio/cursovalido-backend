package com.cursovalido.backend.authentication.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        return valor.codePointCount(0, valor.length()) >= 8
                && valor.getBytes(StandardCharsets.UTF_8).length <= 72
                && valor.codePoints().anyMatch(Character::isUpperCase)
                && valor.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c)
                        && !Character.isWhitespace(c) && !Character.isSpaceChar(c)
                        && !Character.isISOControl(c));
    }
}
