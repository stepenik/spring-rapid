package com.github.vincemann.springrapid.core.proxy;

import com.github.vincemann.springrapid.commons.Lists;
import com.github.vincemann.springrapid.core.model.IdentifiableEntityImpl;
import com.github.vincemann.springrapid.core.service.CrudService;
import com.github.vincemann.springrapid.core.service.jpa.JPACrudService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class CrudServiceExtensionProxyTest {


    @AllArgsConstructor
    @NoArgsConstructor
    class Entity extends IdentifiableEntityImpl<Long> {
        String name;
    }

    interface Service extends CrudService<Entity, Long, JpaRepository<Entity, Long>>, SubInterface, OverlappingInterface{

    }

    class ServiceImpl extends JPACrudService<Entity, Long, JpaRepository<Entity, Long>>
            implements Service{

        @Override
        public Class<?> getTargetClass() {
            return ServiceImpl.class;
        }

        @Override
        public void overlappingMethod() {

        }

        @Override
        public void onceOnlyMethod() {

        }
    }

    interface OverlappingInterface extends SubInterface {
        public void onceOnlyMethod();
    }

    interface SubInterface {
        public void overlappingMethod();
    }

    class FooCrudServiceExtension
            extends ServiceExtension<CrudService>
                    implements CrudServiceExtension<CrudService>{
    }

    class OverlappingExtension extends ServiceExtension<OverlappingInterface> implements OverlappingInterface{

        @Override
        public void onceOnlyMethod() {

        }

        @Override
        public void overlappingMethod() {
            getNext().overlappingMethod();
        }
    }

    class SubExtension extends ServiceExtension<SubInterface> implements SubInterface{
        @Override
        public void overlappingMethod() {
            getNext().overlappingMethod();
        }
    }


    Service proxy;
    @Spy
    Service service;
    
    @Spy
    SubExtension subExtension;
    @Spy
    OverlappingExtension overlappingExtension;
    @Spy
    FooCrudServiceExtension serviceExtension;
    @Mock
    Entity entity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        proxy = ServiceExtensionProxyFactory.create(service, subExtension,serviceExtension,overlappingExtension);
    }

    @Test
    public void invokeOverlappingMethod() throws Throwable {
        InOrder inOrder = new InOrderImpl(Lists.newArrayList(service,subExtension,overlappingExtension));
        proxy.overlappingMethod();
        inOrder.verify(subExtension).overlappingMethod();
        inOrder.verify(overlappingExtension).overlappingMethod();
        inOrder.verify(service).overlappingMethod();
    }

//    @Test
//    public void callMethod_shouldCallExtensionsAfterMethod_with_serviceResult_asLastArg() throws Throwable {
//        Entity result = new Entity("res");
//        Mockito.when(service.save(entity))
//                .thenReturn(result);
//        invokeProxy("save", entity);
//        Mockito.verify(fooCrudServiceExtension).onAfterSave(entity, result);
//    }
//
//    @Test
//    public void call_roleLimitedMethod_with_blacklistedRole_shouldNotCallExtensionMethod() throws Throwable {
//        mockWithRoles(BAD_BOY_ROLE);
//        invokeProxy("save", entity);
//        Mockito.verify(fooCrudServiceExtension,Mockito.never()).onAfterSave(any(Entity.class),any(Entity.class));
//    }
//
//    @Test
//    public void call_roleLimitedMethod_with_requiredRole_shouldCallExtensionMethod() throws Throwable {
//        mockWithRoles(ADMIN_ROLE);
//        invokeProxy("deleteById", 42L);
//        Mockito.verify(fooCrudServiceExtension).onAfterDeleteById(any(Long.class));
//    }
//
//    @Test
//    public void call_roleLimitedMethod_without_requiredRole_shouldNotCallExtensionMethod() throws Throwable {
//        mockWithRoles(NEUTRAL_ROLE);
//        invokeProxy("deleteById", 42L);
//        Mockito.verify(fooCrudServiceExtension,Mockito.never()).onAfterDeleteById(any(Long.class));
//    }
//
//    @Test
//    public void call_roleLimitedMethod_with_required_and_blacklisted_Role_shouldNotCallExtensionMethod() throws Throwable {
//        mockWithRoles(ADMIN_ROLE,SPECIFIC_ADMIN_ROLE);
//        invokeProxy("deleteById", 42L);
//        Mockito.verify(fooCrudServiceExtension,Mockito.never()).onAfterDeleteById(any(Long.class));
//    }
//
//    @Test
//    public void call_roleLimitedMethod_withDontAllowAnon_as_anon_shouldNotCallExtensionMethod() throws Throwable {
//        invokeProxy("update", entity,true);
//        Mockito.verify(fooCrudServiceExtension,Mockito.never()).onAfterUpdate(any(Entity.class),any(Boolean.class),any(Entity.class));
//    }
//
//    private <T> T invokeProxy(String methodName, Object... args) throws Throwable {
//        Method customMethod = Arrays.stream(service.getClass().getMethods())
//                .filter(m -> m.getName().equals(methodName))
//                .findFirst().get();
//        return (T) proxy.invoke(service, customMethod, args);
//    }
//
//    private void mockWithRoles(String... roles) {
//        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("test", "test",
//                Arrays.stream(roles)
//                        .map(r -> new SimpleGrantedAuthority(r))
//                        .collect(Collectors.toSet())
//        );
//        Mockito.when(securityContextMock.getAuthentication()).thenReturn(token);
//        SecurityContextHolder.setContext(securityContextMock);
//    }
//
//    @AfterEach
//    void tearDown() {
//        Mockito.when(securityContextMock.getAuthentication()).thenReturn(null);
//    }
//
//    //see some analog tests in CrudServiceSecurityProxyTest for oder and entityClassArg appending



}