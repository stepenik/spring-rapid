package io.github.vincemann.generic.crud.lib.controller.dtoMapper.idResolver.biDir.abs;

import io.github.vincemann.generic.crud.lib.controller.dtoMapper.idResolver.biDir.testEntities.BiDirEntityChild;
import io.github.vincemann.generic.crud.lib.controller.dtoMapper.idResolver.biDir.testEntities.BiDirEntityParent;
import io.github.vincemann.generic.crud.lib.controller.dtoMapper.idResolver.biDir.testEntities.BiDirSecondEntityParent;
import io.github.vincemann.generic.crud.lib.service.CrudService;
import io.github.vincemann.generic.crud.lib.service.locator.CrudServiceLocator;
import io.github.vincemann.generic.crud.lib.service.exception.NoIdException;
import io.github.vincemann.generic.crud.lib.service.locator.ServiceBeanInfo;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.repository.CrudRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;

@Getter
/**
 * Requires @MockitoSettings(strictness = Strictness.LENIENT) class level annotation on ChildClass
 */
public abstract class BiDirEntityResolverTest {

    @Mock
    private CrudServiceLocator crudServiceLocator;
    @Mock
    private CrudService<BiDirEntityChild,Long, CrudRepository<BiDirEntityChild,Long>> entityChildCrudService;
    @Mock
    private CrudService<BiDirEntityParent,Long,CrudRepository<BiDirEntityParent,Long>> entityParentCrudService;
    @Mock
    private CrudService<BiDirSecondEntityParent,Long,CrudRepository<BiDirSecondEntityParent,Long>> secondEntityParentCrudService;

    private BiDirEntityParent biDirEntityParent = new BiDirEntityParent();
    private BiDirSecondEntityParent biDirSecondEntityParent = new BiDirSecondEntityParent();
    private BiDirEntityChild biDirChild = new BiDirEntityChild();

    @BeforeEach
    public void setUp() throws NoIdException {
        MockitoAnnotations.initMocks(this);

        Long entityParentId = 1L;
        Long secondEntityParentId = 2L;
        Long childId = 3L;
        biDirEntityParent.setId(entityParentId);
        biDirSecondEntityParent.setId(secondEntityParentId);
        biDirChild.setId(childId);

        when(entityParentCrudService.findById(entityParentId))
                .thenReturn(Optional.of(biDirEntityParent));
        when(secondEntityParentCrudService.findById(secondEntityParentId))
                .thenReturn(Optional.of(biDirSecondEntityParent));
        when(entityChildCrudService.findById(childId))
                .thenReturn(Optional.of(biDirChild));

        Map<ServiceBeanInfo,CrudService> classCrudServiceMap = new HashMap<>();
        classCrudServiceMap.put(new ServiceBeanInfo(BiDirEntityParent.class),entityParentCrudService);
        classCrudServiceMap.put(new ServiceBeanInfo(BiDirSecondEntityParent.class),secondEntityParentCrudService);
        classCrudServiceMap.put(new ServiceBeanInfo(BiDirEntityChild.class),entityChildCrudService);

        when(crudServiceLocator.find())
                .thenReturn(classCrudServiceMap);
    }
}
