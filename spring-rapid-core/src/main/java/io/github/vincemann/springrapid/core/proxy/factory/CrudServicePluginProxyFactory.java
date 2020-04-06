package io.github.vincemann.springrapid.core.proxy.factory;

import com.google.common.collect.Lists;
import io.github.vincemann.springrapid.core.model.IdentifiableEntity;
import io.github.vincemann.springrapid.core.proxy.invocationHandler.CrudServicePluginProxy;
import io.github.vincemann.springrapid.core.service.CrudService;
import io.github.vincemann.springrapid.core.service.plugin.CrudServicePlugin;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.util.AopTestUtils;

import java.io.Serializable;
import java.lang.reflect.Proxy;

public class CrudServicePluginProxyFactory {
    //we need the class explicitly here to avoid issues with other proxies. HibernateProxies for example, are not interfaces, so service.getClass returns no interface
    //-> this would make this crash
    public static <Id extends Serializable, E extends IdentifiableEntity<Id>, S extends CrudService<E, Id, ? extends CrudRepository<E, Id>>> S
    create(S crudService, CrudServicePlugin<? super E, ? super Id>... plugins) {
        //resolve spring aop proxy
        S unproxied = AopTestUtils.getUltimateTargetObject(crudService);
        S proxyInstance = (S) Proxy.newProxyInstance(
                unproxied.getClass().getClassLoader(), unproxied.getClass().getInterfaces(),
                new CrudServicePluginProxy(unproxied, Lists.newArrayList(plugins)));

        return proxyInstance;
    }


}