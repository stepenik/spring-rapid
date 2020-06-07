package io.github.vincemann.springlemon.auth.config;

import io.github.vincemann.springlemon.auth.domain.AbstractUserRepository;
import io.github.vincemann.springlemon.auth.security.LemonAclPlugin;
import io.github.vincemann.springlemon.auth.service.LemonService;
import io.github.vincemann.springlemon.auth.security.LemonServiceSecurityRule;
import io.github.vincemann.springrapid.acl.plugin.CleanUpAclPlugin;
import io.github.vincemann.springrapid.acl.proxy.create.CrudServiceSecurityProxyFactory;
import io.github.vincemann.springrapid.acl.service.AclManaging;
import io.github.vincemann.springrapid.acl.service.LocalPermissionService;
import io.github.vincemann.springrapid.acl.service.MockAuthService;
import io.github.vincemann.springrapid.acl.service.Secured;
import io.github.vincemann.springrapid.core.proxy.factory.CrudServicePluginProxyFactory;
import io.github.vincemann.springrapid.core.slicing.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.acls.model.MutableAclService;

@ServiceConfig
@Slf4j
public class LemonServiceAutoConfiguration {

    @Autowired
    CrudServiceSecurityProxyFactory securityProxyFactory;

    @Autowired
    LocalPermissionService permissionService;

    @Autowired
    MutableAclService mutableAclService;

    @Autowired
    AbstractUserRepository<?,?> userRepository;

    @Autowired
    MockAuthService mockAuthService;

    public LemonServiceAutoConfiguration() {
        log.info("Created");
    }

    @ConditionalOnMissingBean(LemonServiceSecurityRule.class)
    @Bean
    public LemonServiceSecurityRule lemonServiceSecurityRule(AbstractUserRepository<?,?> repository){
        return new LemonServiceSecurityRule(repository);
    }

    @Bean
    @ConditionalOnMissingBean(LemonAclPlugin.class)
    public LemonAclPlugin lemonAclPlugin(){
        return new LemonAclPlugin(permissionService,mutableAclService,mockAuthService,userRepository);
    }

    @ConditionalOnMissingBean(name = "aclManagingLemonService")
    @Bean
    @AclManaging
    public LemonService<?,?,?> aclManagingLemonService(LemonService<?,?,?> service,
//                                                                            AdminFullAccessAclPlugin adminFullAccess,
//                                                                            AuthenticatedFullAccessAclPlugin authenticatedFullAccessAclPlugin,
                                                       CleanUpAclPlugin cleanUpAclPlugin){
        return CrudServicePluginProxyFactory.create(service/*,adminFullAccess*/,lemonAclPlugin(),/*authenticatedFullAccessAclPlugin,*/cleanUpAclPlugin);
    }


    @ConditionalOnMissingBean(name = "securedLemonService")
    @Bean
    @Secured
    public LemonService<?,?,?> securedLemonService(@AclManaging LemonService<?,?,?> service,
                                                LemonServiceSecurityRule securityRule){
        return securityProxyFactory.create(service,securityRule);
    }
}