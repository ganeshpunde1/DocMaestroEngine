package com.maestro.po.ms.inference.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;

import com.maestro.po.ms.inference.model.annotation.EitherRegexOrAnswerCheck;
import com.maestro.po.ms.inference.model.rest.FieldMatchingRule;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EitherRegexOrAnswerValidation implements ConstraintValidator<EitherRegexOrAnswerCheck, FieldMatchingRule>
{
    private String regexField;

    private String answersField;

    @Override
    public void initialize(EitherRegexOrAnswerCheck constraintAnnotation)
    {
        this.regexField = constraintAnnotation.stringFieldName();
        this.answersField = constraintAnnotation.listFieldName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValid(FieldMatchingRule value, ConstraintValidatorContext context)
    {
        if (value == null)
        {
            return true; // No validation needed if object is null
        }

        try
        {
            // Using Spring's BeanWrapperImpl for easier field access
            String regexObjValue = (String) new BeanWrapperImpl(value).getPropertyValue(regexField);
            List<String> answersObjValue = (List<String>) new BeanWrapperImpl(value).getPropertyValue(answersField);

            // If either value is null, just return true
            if (StringUtils.isBlank(regexObjValue) || answersObjValue == null || CollectionUtils.isEmpty(answersObjValue))
                return true;

            return false;
        }
        catch (Exception e)
        {
            return false;
        }

    }

}
