package com.maestro.po.ms.inference.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.maestro.po.ms.inference.filter.InferenceRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig
{

    @Bean
    public FilterRegistrationBean<InferenceRequestLoggingFilter> servletRegistrationBean2()
    {
        final FilterRegistrationBean<InferenceRequestLoggingFilter> registrationBean = new FilterRegistrationBean<InferenceRequestLoggingFilter>();
        InferenceRequestLoggingFilter filter = new InferenceRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludeClientInfo(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(100000);
        filter.setIncludeHeaders(true);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        registrationBean.setFilter(filter);
        registrationBean.setOrder(3);
        return registrationBean;
    }
}