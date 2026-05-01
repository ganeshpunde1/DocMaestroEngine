package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.Base64SizeValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = Base64SizeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Base64SizeCheck
{
    int maxSize() default -1;
    
    String maxSizeProperty() default "";
    
    /**
     * How many bytes to allow.
     * Eg 4500000 is 4.5 miB
     * @return
     */
    int minSize() default -1;
    
    String minSizeProperty() default "";
    
    String message() default "Value of the Base64 is too large or too small";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
