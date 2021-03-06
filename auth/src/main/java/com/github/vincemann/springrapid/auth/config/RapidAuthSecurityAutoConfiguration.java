package com.github.vincemann.springrapid.auth.config;

import com.github.vincemann.springrapid.auth.domain.RapidAuthAuthenticatedPrincipal;
import com.github.vincemann.springrapid.auth.security.*;
import com.github.vincemann.springrapid.auth.service.token.AuthorizationTokenService;
import com.github.vincemann.springrapid.auth.service.token.RapidJwtAuthorizationTokenService;
import com.github.vincemann.springrapid.core.security.RapidSecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RapidAuthSecurityAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean(AuthorizationTokenService.class)
    public AuthorizationTokenService<RapidAuthAuthenticatedPrincipal> authorizationTokenService(){
        return new RapidJwtAuthorizationTokenService();
    }

    @Bean
    @ConditionalOnMissingBean(RapidSecurityContext.class)
    public RapidSecurityContext<?> rapidSecurityContext(){
        return new RapidAuthSecurityContext();
    }


    // there can only be ONE Factory
    // if user wishes to create AuthPrincipal differently or with diff subtypes he can define own bean
    @Bean
    @ConditionalOnMissingBean(AuthenticatedPrincipalFactory.class)
    public AuthenticatedPrincipalFactory authenticatedPrincipalFactory(){
        return new RapidAuthenticatedPrincipalFactory<>();
    }


    @Bean
    @ConditionalOnMissingBean(JwtClaimsPrincipalConverter.class)
    public JwtClaimsPrincipalConverter<RapidAuthAuthenticatedPrincipal> jwtClaimsPrincipalConverter(){
        return new RapidJwtClaimsPrincipalConverter();
    }

    @Bean
    @ConditionalOnMissingBean(RapidAuthSecurityContextChecker.class)
    public RapidAuthSecurityContextChecker lemonSecurityContextChecker(){
        return new RapidAuthSecurityContextChecker();
    }
}
