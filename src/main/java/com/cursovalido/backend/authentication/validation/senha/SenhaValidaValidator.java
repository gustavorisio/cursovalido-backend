package com.cursovalido.backend.authentication.validation.senha;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        // Campo vazio fica a cargo de @NotBlank, que traz a mensagem de preenchimento obrigatorio.
        if (valor == null || valor.isEmpty()) {
            return true;
        }
        var problema = RegrasSenha.primeiroProblema(valor);
        if (problema.isEmpty()) {
            return true;
        }
        // Informa exatamente qual regra faltou, em vez de uma mensagem generica.
        if (contexto != null) {
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(problema.get()).addConstraintViolation();
        }
        return false;
    }
}
