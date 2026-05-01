package com.maestro.po.ms.inference.service;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.maestro.po.ms.inference.annotations.PropertyValueCheck;
import com.maestro.po.ms.inference.constants.ApplicationConstants;
import com.maestro.po.ms.inference.provider.SpringContextHolder;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyValueValidator implements ConstraintValidator<PropertyValueCheck, String>
{
    private List<String> props;

    public void setProps(List<String> props) { this.props = props; }

    @Override
    public void initialize(PropertyValueCheck ev)
    {
        String baseProp = SpringContextHolder.getApplicationContext().getEnvironment().getProperty(ev.property());
        if (StringUtils.isBlank(baseProp))
        {
            log.error("{}: property '{}' is missing or blank", ApplicationConstants.PROPERTY_NOT_FOUND_ERROR_CODE, ev.property());
            this.props = Collections.emptyList();
        }
        else
        {
            this.props = List.of(baseProp.split(","));
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null || value.isBlank())
        {
            return true;
        }
        
        
        return props.contains(value);
    }

}
