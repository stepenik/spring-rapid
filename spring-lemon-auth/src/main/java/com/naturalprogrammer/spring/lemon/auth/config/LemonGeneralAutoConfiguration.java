package com.naturalprogrammer.spring.lemon.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalprogrammer.spring.lemon.auth.LemonProperties;
import com.naturalprogrammer.spring.lemon.auth.util.LemonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties
public class LemonGeneralAutoConfiguration {

    public LemonGeneralAutoConfiguration() {
        log.info("Created");
    }

    /**
     * Spring Lemon related properties
     */
    @ConfigurationProperties(prefix="lemon",ignoreUnknownFields = false)
    @Bean
    public LemonProperties lemonProperties() {

        log.info("Configuring LemonProperties");
        return new LemonProperties();
    }

    /**
     * Configures LemonUtils
     */
    @Bean
    public LemonUtils lemonUtils() {
        log.info("Configuring LemonUtils");
        return new LemonUtils();
    }
}