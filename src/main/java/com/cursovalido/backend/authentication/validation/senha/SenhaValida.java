package com.cursovalido.backend.authentication.validation.senha;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String MENSAGEM = "Use pelo menos uma letra maiúscula, uma letra minúscula e um caractere especial, como ! ou @, com no máximo 72 caracteres.";

    String message() default MENSAGEM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
