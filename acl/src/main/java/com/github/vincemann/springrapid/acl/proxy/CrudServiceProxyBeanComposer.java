package com.github.vincemann.springrapid.acl.proxy;

import com.github.vincemann.springrapid.core.util.Lists;
import com.github.vincemann.springrapid.core.proxy.ServiceExtensionProxyBuilder;
import com.github.vincemann.springrapid.core.service.SimpleCrudService;
import com.github.vincemann.springrapid.core.proxy.BasicServiceExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Used to create {@link SimpleCrudService} Proxy's dynamically by resolving {@link ConfigureProxies} annotation.
 * @see ConfigureProxies
 */
@Slf4j
public class CrudServiceProxyBeanComposer implements BeanPostProcessor, ApplicationContextAware {

    private DefaultListableBeanFactory beanFactory;
    private SecurityServiceExtensionProxyBuilderFactory securityProxyBuilderFactory;

    @Autowired
    public CrudServiceProxyBeanComposer(SecurityServiceExtensionProxyBuilderFactory securityProxyBuilderFactory) {
        this.securityProxyBuilderFactory = securityProxyBuilderFactory;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.beanFactory = ((DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //log.debug("postProcessing bean : " + beanName);
        Object unwrappedBean = AopTestUtils.getUltimateTargetObject(bean);
        if(unwrappedBean instanceof SimpleCrudService){
            ConfigureProxies proxiesConfig = AnnotationUtils.findAnnotation(unwrappedBean.getClass(), ConfigureProxies.class);
            if(proxiesConfig==null){
                return bean;
            }
            boolean primaryBeanRegistered = beanFactory.getBeanDefinition(beanName).isPrimary();
            Class serviceInterface = resolveServiceInterface(unwrappedBean, beanName);
            List<Proxy> proxies = Lists.newArrayList(proxiesConfig.value());
            ArrayList<SecurityProxy> securityProxies = Lists.newArrayList(proxiesConfig.securityProxies());
            log.debug("Identified Proxies of bean: " + beanName + " : " + proxies);
            log.debug("Identified SecurityProxies of bean: " + beanName + " : " + securityProxies);
            SimpleCrudService lastProxiedBean = null;
            for (Proxy proxy : proxies) {
                if(proxy.primary()){
                    if(primaryBeanRegistered){
                        throw new IllegalArgumentException("Multiple ProxyBeans marked as primary");
                    }
                    primaryBeanRegistered=true;
                }
                GenericBeanDefinition proxyBeanDef
                        = createBeanDef(proxy.qualifiers(), proxy.primary(), ((Class<? extends SimpleCrudService>) serviceInterface));
                String proxyBeanName = resolveProxyName(proxy.qualifiers(),proxy.primary(),proxy.name(),unwrappedBean.getClass());
                log.debug("creating proxyBean with name: " + proxyBeanName);
                SimpleCrudService proxiedBean;
                if(lastProxiedBean==null){
                     proxiedBean= ((SimpleCrudService) bean);
                }else {
                    proxiedBean = lastProxiedBean;
                }
                SimpleCrudService proxyBean = new ServiceExtensionProxyBuilder<>(proxiedBean)
                        .addServiceExtensions(resolveExtensions(proxy.plugins()).toArray(new BasicServiceExtension[0])).build();

                log.trace("creating proxyBean : " + proxyBean);
                log.trace("Registering beanDef of proxyBean first: " + proxyBeanDef);
                beanFactory.registerBeanDefinition(proxyBeanName,proxyBeanDef);
                beanFactory.registerSingleton(proxyBeanName,proxyBean);
                log.trace("registered proxyBean.");
                lastProxiedBean = proxyBean;
            }

            for (SecurityProxy securityProxy : securityProxies) {
                if(securityProxy.primary()){
                    if(primaryBeanRegistered){
                        throw new IllegalArgumentException("Multiple ProxyBeans marked as primary");
                    }
                    primaryBeanRegistered=true;
                }
                List<Class<? extends BasicServiceExtension>> pluginTypes = Lists.newArrayList(securityProxy.plugins());
                if(!pluginTypes.isEmpty()){
                    lastProxiedBean = new ServiceExtensionProxyBuilder<>(lastProxiedBean)
                            .addServiceExtensions(resolveExtensions(securityProxy.plugins()).toArray(new BasicServiceExtension[0]))
                            .build();
                }
                GenericBeanDefinition beanDef
                        = createBeanDef(securityProxy.qualifiers(), securityProxy.primary(), ((Class<? extends SimpleCrudService>) serviceInterface));
                String proxyBeanName = resolveProxyName(securityProxy.qualifiers(),securityProxy.primary(),securityProxy.name(),unwrappedBean.getClass());
                SimpleCrudService securityProxyBean = securityProxyBuilderFactory.create(lastProxiedBean)
                        .addServiceExtensions(resolveRules(securityProxy).toArray(new SecurityServiceExtension[0]))
                        .build();
                log.debug("Creating proxyBean with name: " + proxyBeanName);
                log.debug("Creating security proxyBean : " + securityProxyBean);
                log.debug("Registering beanDef of securityProxyBean first: " + beanDef);
                beanFactory.registerBeanDefinition(proxyBeanName,beanDef);
                beanFactory.registerSingleton(proxyBeanName,securityProxyBean);
                log.trace("registered securityProxyBean.");
                lastProxiedBean = securityProxyBean;
            }
        }
        return bean;
    }


    private Class resolveServiceInterface(Object bean, String beanName){
        String entityName = ((SimpleCrudService) bean).getEntityClass().getSimpleName();
        String interfaceName = entityName + "Service";
        Optional<Class<?>> serviceInterfaceClass = Lists.newArrayList(bean.getClass().getInterfaces()).stream()
                .filter(i -> i.getSimpleName().equals(interfaceName))
                .findFirst();
        Assert.isTrue(serviceInterfaceClass.isPresent(),"Could not find interface named: " + interfaceName +" for Service bean: " + beanName +" please create interface following namingConvention : 'entityName+Service'");
        return serviceInterfaceClass.get();
    }

    private GenericBeanDefinition createBeanDef(Class<? extends Annotation>[] qualifiers,boolean primary, Class<? extends SimpleCrudService> beanClass){
        final GenericBeanDefinition serviceBeanDef = new GenericBeanDefinition();
        for (Class<? extends Annotation> qualifier : qualifiers) {
            Assert.isTrue(qualifier.isAnnotationPresent(Qualifier.class));
            serviceBeanDef.addQualifier(new AutowireCandidateQualifier(qualifier));
        }
        serviceBeanDef.setPrimary(primary);
        serviceBeanDef.setBeanClass(beanClass);
        return serviceBeanDef;
    }

    private String resolveProxyName(Class<? extends Annotation>[] qualifiers,boolean primary,String beanName, Class beanType){
        String name = beanName;
        if(name.isEmpty()){
            String prefix;
            if(primary){
                prefix = "primary";
            }else {
                StringBuilder sb = new StringBuilder();
                Arrays.stream(qualifiers)
                        .forEach(type -> sb.append(type.getSimpleName()));
                prefix = sb.toString();
            }
            return prefix+beanType.getSimpleName();
        }
        return name;
    }

    private List<BasicServiceExtension> resolveExtensions(Class<? extends BasicServiceExtension>[] pluginTypeArray){
        List<BasicServiceExtension> plugins = new ArrayList<>();
        ArrayList<Class<? extends BasicServiceExtension>> pluginTypes = Lists.newArrayList(pluginTypeArray);
        for (Class<? extends BasicServiceExtension> pluginType : pluginTypes) {
            BasicServiceExtension plugin = beanFactory.getBean(pluginType);
            beanFactory.autowireBean(plugin);
            //beanFactory.autowireBeanProperties();
            plugins.add(plugin);
        }
        return plugins;
    }

    private List<SecurityServiceExtension> resolveRules(SecurityProxy proxy){
        List<SecurityServiceExtension> rules = new ArrayList<>();
        ArrayList<Class<? extends SecurityServiceExtension>> pluginTypes = Lists.newArrayList(proxy.rules());
        for (Class<? extends SecurityServiceExtension> ruleType : pluginTypes) {
            SecurityServiceExtension rule = beanFactory.getBean(ruleType);
            rules.add(rule);
        }
        return rules;
    }

}
