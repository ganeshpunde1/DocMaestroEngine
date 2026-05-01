package com.maestro.po.ms.inference.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maestro.po.ms.inference.annotations.EnumValueCheck;
import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.model.annotation.EnumParentEntity;
import com.maestro.po.ms.inference.provider.SpringContextHolder;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnumValueValidator implements ConstraintValidator<EnumValueCheck, String>
{

    JpaRepository<? extends EnumParentEntity, String> parentEnumRepository;
   
    @Override
    public void initialize(EnumValueCheck ev)
    {
        try
        {
            this.parentEnumRepository = (JpaRepository<? extends EnumParentEntity, String>)
                SpringContextHolder.getApplicationContext().getBean(ev.enumRepository());
        }
        catch (Exception e)
        {
            log.error("{}: failed to load enum repository '{}' - {}",
                ApplicationConstants.SPRING_CONTEXT_NULL_ERROR_CODE, ev.enumRepository(), e.getMessage());
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (StringUtils.isBlank(value)) return true;

        if (parentEnumRepository == null)
        {
            log.error("{}: parentEnumRepository is null, skipping validation", ApplicationConstants.SPRING_CONTEXT_NULL_ERROR_CODE);
            return false;
        }
        
        Object parentEnum = null;

        try
        {
            parentEnum = parentEnumRepository.findById(value).orElse(null);
        }
        catch (Exception e)
        {
            log.error("Validation failed on " + value);
        }
        
        return parentEnum != null;
    }

}
