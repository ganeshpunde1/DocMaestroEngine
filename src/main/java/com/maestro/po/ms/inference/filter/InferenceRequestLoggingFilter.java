package com.maestro.po.ms.inference.filter;

import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class InferenceRequestLoggingFilter extends CommonsRequestLoggingFilter
{
    @Override
    protected void beforeRequest(HttpServletRequest request, String message)
    {
        logger.debug(message != null ? Base64.getEncoder().encodeToString(message.getBytes()) : message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message)
    {
        logger.debug(message != null ? Base64.getEncoder().encodeToString(message.getBytes()) : message);
    }
}
