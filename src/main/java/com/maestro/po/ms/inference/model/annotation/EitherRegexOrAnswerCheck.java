package com.maestro.po.ms.inference.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.maestro.po.ms.inference.service.EitherRegexOrAnswerValidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = EitherRegexOrAnswerValidation.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EitherRegexOrAnswerCheck
{
    String message() default "Both answers and regex cannot be given for the same field";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String stringFieldName() default "regex";

    String listFieldName() default "answers";

}
