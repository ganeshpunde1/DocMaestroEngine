package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.PropertyValueValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = PropertyValueValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyValueCheck
{
    
    String property();
    
    String message() default "No corresponding model value";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
    
}
