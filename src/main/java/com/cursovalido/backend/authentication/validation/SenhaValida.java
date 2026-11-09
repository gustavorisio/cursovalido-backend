package com.cursovalido.backend.authentication.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String message() default "A senha deve ter pelo menos 8 caracteres, uma letra maiuscula, um caractere especial e no maximo 72 bytes em UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
