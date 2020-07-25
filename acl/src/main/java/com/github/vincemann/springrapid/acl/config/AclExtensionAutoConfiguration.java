package com.github.vincemann.springrapid.acl.config;

import com.github.vincemann.springrapid.acl.service.extensions.AdminFullAccessAclServiceExtension;
import com.github.vincemann.springrapid.acl.service.extensions.AuthenticatedFullAccessAclServiceExtension;
import com.github.vincemann.springrapid.acl.service.extensions.CleanUpAclServiceExtension;
import com.github.vincemann.springrapid.acl.service.extensions.InheritParentAclServiceExtension;
import com.github.vincemann.springrapid.acl.service.LocalPermissionService;
import com.github.vincemann.springrapid.acl.service.MockAuthService;
import com.github.vincemann.springrapid.core.config.RapidJacksonAutoConfiguration;
import com.github.vincemann.springrapid.core.slicing.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.acls.model.MutableAclService;

@ServiceConfig
@Slf4j
@AutoConfigureAfter(RapidJacksonAutoConfiguration.class)
public class AclExtensionAutoConfiguration {

    public AclExtensionAutoConfiguration() {
        log.info("Created");
    }


    @ConditionalOnMissingBean(LocalPermissionService.class)
    @Bean
    public LocalPermissionService localPermissionService(MutableAclService aclService){
        return new LocalPermissionService(aclService);
    }

    @ConditionalOnMissingBean(AdminFullAccessAclServiceExtension.class)
    @Bean
    public AdminFullAccessAclServiceExtension adminFullAccessAclExtension(MutableAclService mutableAclService, MockAuthService mockAuthService){
        return new AdminFullAccessAclServiceExtension(localPermissionService(mutableAclService),mutableAclService,mockAuthService);
    }

    @ConditionalOnMissingBean(AuthenticatedFullAccessAclServiceExtension.class)
    @Bean
    public AuthenticatedFullAccessAclServiceExtension authenticatedFullAccessAclExtension(MutableAclService mutableAclService, MockAuthService mockAuthService){
        return new AuthenticatedFullAccessAclServiceExtension(localPermissionService(mutableAclService),mutableAclService,mockAuthService);
    }

    @Bean
    @ConditionalOnMissingBean(InheritParentAclServiceExtension.class)
    public InheritParentAclServiceExtension inheritParentAclExtension(MutableAclService aclService, MockAuthService mockAuthService){
        return new InheritParentAclServiceExtension(localPermissionService(aclService),aclService,mockAuthService);
    }

    @Bean
    @ConditionalOnMissingBean(CleanUpAclServiceExtension.class)
    public CleanUpAclServiceExtension cleanUpAclExtension(MutableAclService aclService, MockAuthService mockAuthService){
        return new CleanUpAclServiceExtension(localPermissionService(aclService),aclService,mockAuthService);
    }
}