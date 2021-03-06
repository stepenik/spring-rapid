package com.github.vincemann.springrapid.coredemo.lib.controller;

import com.github.vincemann.springrapid.core.controller.CrudController;
import com.github.vincemann.springrapid.core.controller.dto.mapper.context.CrudDtoMappingContextBuilder;
import com.github.vincemann.springrapid.core.controller.dto.mapper.context.DtoMappingContext;

public class ExampleController
        extends CrudController<ExampleEntity,Long,ExampleService>
{
    private DtoMappingContext mock;

    public ExampleController(DtoMappingContext dtoMappingContext) {
        super();
        this.mock = dtoMappingContext;
    }

    @Override
    protected DtoMappingContext provideDtoMappingContext(CrudDtoMappingContextBuilder builder) {
        return mock;
    }

}
