package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.RegexValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = RegexValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RegexCheck
{    
    String message() default "value is not a regex expression";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
