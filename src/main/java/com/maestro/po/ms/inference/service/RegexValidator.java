package com.maestro.po.ms.inference.service;

import java.util.regex.Pattern;

import com.maestro.po.ms.inference.annotations.RegexCheck;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegexValidator implements ConstraintValidator<RegexCheck, String>
{

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank())
        {
            return true;
        }

        try
        {
            Pattern.compile(value);
            return true;
        }
        catch (Exception e)
        {
            log.error("Input: " + value + " is not a valid regex expression encoded string");
            return false;
        }
    }

}
