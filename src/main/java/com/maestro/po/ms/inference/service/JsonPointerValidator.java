package com.maestro.po.ms.inference.service;

import com.fasterxml.jackson.core.JsonPointer;
import com.maestro.po.ms.inference.annotations.JsonPointerCheck;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JsonPointerValidator implements ConstraintValidator<JsonPointerCheck, String>
{

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank()) return true;
        
        try {
            JsonPointer.compile(value);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

}
