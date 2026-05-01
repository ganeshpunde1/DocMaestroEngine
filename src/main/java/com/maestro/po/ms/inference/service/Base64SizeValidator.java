package com.maestro.po.ms.inference.service;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.maestro.po.ms.inference.annotations.Base64SizeCheck;
import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.provider.SpringContextHolder;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Base64SizeValidator implements ConstraintValidator<Base64SizeCheck, String>
{

    private boolean skipMinSizeComparison;
    private boolean skipMaxSizeComparison;

    private long maxSize;
    private long minSize;

    @Override
    public void initialize(Base64SizeCheck check)
    {
        skipMaxSizeComparison = false;
        skipMinSizeComparison = false;
        if (check.maxSize() <= 0 && StringUtils.isBlank(check.maxSizeProperty()))
        {
            skipMaxSizeComparison = true;
        }

        if (check.minSize() <= 0 && StringUtils.isBlank(check.minSizeProperty()))
        {
            skipMinSizeComparison = true;
        }

        if (!StringUtils.isBlank(check.maxSizeProperty()))
        {
            String baseProp = SpringContextHolder.getApplicationContext().getEnvironment().getProperty(check.maxSizeProperty());
            if (StringUtils.isBlank(baseProp))
            {
                log.error("{}: property '{}' not found for maxSizeProperty", ApplicationConstants.PROPERTY_NOT_FOUND_ERROR_CODE, check.maxSizeProperty());
                skipMaxSizeComparison = true;
            }
            else
            {
                try
                {
                    maxSize = Long.valueOf(baseProp);
                }
                catch (NumberFormatException e)
                {
                    log.error("{}: property '{}' could not be parsed as Long - {}", ApplicationConstants.PROPERTY_PARSE_ERROR_CODE, check.maxSizeProperty(), e.getMessage());
                    skipMaxSizeComparison = true;
                }
            }
        }
        else if (check.maxSize() > 0)
        {
            maxSize = check.maxSize();
        }

        if (!StringUtils.isBlank(check.minSizeProperty()))
        {
            String baseProp = SpringContextHolder.getApplicationContext().getEnvironment().getProperty(check.minSizeProperty());
            if (StringUtils.isBlank(baseProp))
            {
                log.error("{}: property '{}' not found for minSizeProperty", ApplicationConstants.PROPERTY_NOT_FOUND_ERROR_CODE, check.minSizeProperty());
                skipMinSizeComparison = true;
            }
            else
            {
                try
                {
                    minSize = Long.valueOf(baseProp);
                }
                catch (NumberFormatException e)
                {
                    log.error("{}: property '{}' could not be parsed as Long - {}", ApplicationConstants.PROPERTY_PARSE_ERROR_CODE, check.minSizeProperty(), e.getMessage());
                    skipMinSizeComparison = true;
                }
            }
        }
        else if (check.minSize() > 0)
        {
            minSize = check.minSize();
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank())
        {
            return true;
        }
        
        byte[] decodedBytes = null;
        
        try
        {
            decodedBytes = Base64.getDecoder().decode(value);
        }
        catch (Exception e)
        {
            log.error("Input: " + value + " is not a valid Base64 encoded string");
            return true;
        }
        
        long sizeOf = decodedBytes.length;
        
        if (!this.skipMaxSizeComparison && (sizeOf > maxSize))
        {
            return false;
        }
        
        if (!this.skipMinSizeComparison && (sizeOf < minSize))
        {
            return false;
        }

        return true;
    }

}
