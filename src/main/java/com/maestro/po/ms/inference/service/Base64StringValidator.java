package com.maestro.po.ms.inference.service;

import java.util.Base64;

import com.maestro.po.ms.inference.annotations.Base64Check;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Base64StringValidator implements ConstraintValidator<Base64Check, String>
{

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank()) return true;
        
        try {
            Base64.getDecoder().decode(value);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

}
