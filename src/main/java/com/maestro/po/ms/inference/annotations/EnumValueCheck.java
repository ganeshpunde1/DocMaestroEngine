package com.maestro.po.ms.inference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maestro.po.ms.inference.model.annotation.EnumParentEntity;
import com.maestro.po.ms.inference.service.EnumValueValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = EnumValueValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValueCheck
{
    
    Class<? extends JpaRepository<? extends EnumParentEntity, String>> enumRepository();
    
    String message() default "No corresponding enum value";
    
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
    
}
