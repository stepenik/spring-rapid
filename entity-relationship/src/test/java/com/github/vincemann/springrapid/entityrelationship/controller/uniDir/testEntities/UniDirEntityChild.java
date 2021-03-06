package com.github.vincemann.springrapid.entityrelationship.controller.uniDir.testEntities;

import com.github.vincemann.springrapid.core.model.IdentifiableEntityImpl;
import com.github.vincemann.springrapid.entityrelationship.model.child.UniDirChild;
import com.github.vincemann.springrapid.entityrelationship.model.parent.annotation.UniDirParentEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UniDirEntityChild extends IdentifiableEntityImpl<Long> implements UniDirChild {
    @UniDirParentEntity
    private UniDirEntityChildsParent uniDirEntityChildsParent;
}
