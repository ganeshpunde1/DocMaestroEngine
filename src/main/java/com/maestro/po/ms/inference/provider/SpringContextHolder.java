package com.maestro.po.ms.inference.provider;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.maestro.po.ms.inference.constants.ApplicationConstants;

/**
 * Holds a static reference to the Spring {@link ApplicationContext},
 * enabling non-Spring-managed classes to access beans and environment properties.
 *
 * @author Ganesh Punde
 */
@Component
public class SpringContextHolder implements ApplicationContextAware
{
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
    	storeApplicationContext(applicationContext);
    }

    private static void storeApplicationContext(ApplicationContext applicationContext)
    {
        SpringContextHolder.applicationContext = applicationContext;
    }
    
    /**
     * Returns the stored Spring ApplicationContext.
     *
     * @return the application context
     */
    public static ApplicationContext getApplicationContext()
    {
        if (applicationContext == null)
            throw new IllegalStateException(
                ApplicationConstants.SPRING_CONTEXT_NULL_ERROR_CODE + ": " + ApplicationConstants.SPRING_CONTEXT_NULL_ERROR_MSG);
        return applicationContext;
    }
}
