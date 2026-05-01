package com.maestro.po.ms.inference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.maestro.po.ms.inference.filter.Slf4jMDCFilter;

@Configuration
@ConfigurationProperties(prefix = "config.slf4jfilter")
public class Slf4jMDCFilterConfiguration
{
    @Bean
    public FilterRegistrationBean<Slf4jMDCFilter> servletRegistrationBean()
    {
        final FilterRegistrationBean<Slf4jMDCFilter> registrationBean = new FilterRegistrationBean<Slf4jMDCFilter>();
        final Slf4jMDCFilter log4jMDCFilterFilter = new Slf4jMDCFilter();
        registrationBean.setFilter(log4jMDCFilterFilter);
        registrationBean.setOrder(2);
        return registrationBean;
    }
}