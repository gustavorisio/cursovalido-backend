package com.cursovalido.backend.authentication.validation.senha;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String MENSAGEM = "A senha deve conter ao menos uma letra maiuscula, uma minuscula e um caractere especial, com no maximo 72 bytes.";

    String message() default MENSAGEM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
