package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.JsonPointerValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = JsonPointerValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPointerCheck
{    
    String message() default "value is not a json pointer expression";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
