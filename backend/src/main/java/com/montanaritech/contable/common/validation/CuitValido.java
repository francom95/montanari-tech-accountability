package com.montanaritech.contable.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CuitValidoValidator.class)
public @interface CuitValido {
    String message() default "CUIT inválido (formato XX-XXXXXXXX-X con dígito verificador incorrecto)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
