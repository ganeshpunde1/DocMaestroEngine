package com.maestro.po.ms.inference.config;

import java.util.Base64;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class MethodExecutionLoggingAspect
{
    @Pointcut("execution(* com.maestro..*(..)) && !annotation(com.maestro.nyia.po.ms.inference.config.SkipMethodLogging) && !execution(* com.maestro.nyia.po.ms.inference.config.*(..))")    public void allMaestroMethods()
    {
    }

    @Around("allMaestroMethods()")
    public Object measureAndLogExecutionTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable
    {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        final StopWatch stopWatch = new StopWatch();

        // calculate method execution time
        stopWatch.start();
        Object result = proceedingJoinPoint.proceed();
        stopWatch.stop();

        // Log method execution time
        log.info("InferenceService - Execution time of {}.{} :: {} ms", methodSignature.getDeclaringType().getSimpleName(), methodSignature.getName(),
                stopWatch.getTotalTimeMillis());
        return result;
    }

    @AfterReturning(value = "allMaestroMethods()", returning = "result")
    public void logReturnValueAsBase64(JoinPoint joinPoint, Object result)
    {
        try
        {
            log.info("InferenceService - commonPoint {} return value : {}", joinPoint.toShortString(),
                    Base64.getEncoder().encodeToString(Json.pretty(result).toString().getBytes()));
        }
        catch (Exception e)
        {
            log.info("InferenceService - commonPoint {} return value : {}", joinPoint.toShortString(), result);
        }
    }

}
