package com.github.vincemann.springrapid.authdemo.service;

import com.github.vincemann.springrapid.core.service.CrudService;
import com.github.vincemann.springrapid.authdemo.model.PetType;
import com.github.vincemann.springrapid.core.slicing.components.ServiceComponent;

@ServiceComponent
public interface PetTypeService extends CrudService<PetType,Long> {
}