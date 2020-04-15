package io.github.vincemann.springrapid.acl.plugin;

import io.github.vincemann.springrapid.acl.service.LocalPermissionService;
import io.github.vincemann.springrapid.core.model.IdentifiableEntity;
import io.github.vincemann.springrapid.core.proxy.CalledByProxy;
import io.github.vincemann.springrapid.core.service.exception.EntityNotFoundException;
import io.github.vincemann.springrapid.core.service.exception.NoIdException;
import io.github.vincemann.springrapid.core.service.plugin.CrudServicePlugin;
import io.github.vincemann.springrapid.core.slicing.components.ServiceComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Getter
@Slf4j
/**
 * Removes Acl's on delete, if existing.
 */
@ServiceComponent
public abstract class CleanUpAclPlugin<E extends IdentifiableEntity<Id>, Id extends Serializable>
        extends CrudServicePlugin<E,Id> {

    private LocalPermissionService permissionService;
    private MutableAclService mutableAclService;
    @Setter
    private boolean deleteCascade = true;

    public CleanUpAclPlugin(LocalPermissionService permissionService, MutableAclService mutableAclService) {
        this.permissionService = permissionService;
        this.mutableAclService = mutableAclService;
    }

    @Transactional
    @CalledByProxy
    public void onAfterDeleteById(Id id,Class entityClass) throws EntityNotFoundException, NoIdException {
        deleteAcl(id,entityClass);
    }

    private void deleteAcl(Id id, Class entityClass){
        log.debug("deleting acl for entity with id: " + id + " and class: " + entityClass);
        //delete acl as well
        ObjectIdentity oi = new ObjectIdentityImpl(entityClass, id);
        log.debug("ObjectIdentity getting deleted: " + oi);
        //todo delete children ist nur richtig wenn ich wirklich one to n habe mit Delete Cascade!
        getMutableAclService().deleteAcl(oi,deleteCascade);
        log.debug("Acl successfully deleted");
    }

}
