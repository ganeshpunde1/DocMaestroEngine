package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.Base64StringValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = Base64StringValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
/**
 * Validates string is properly base64 encoded
 */
public @interface Base64Check
{    
    String message() default "Not proper base64 format";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
    
}
