package io.github.vincemann.demo.lib.controller;

import io.github.vincemann.generic.crud.lib.controller.dtoMapper.context.DtoMappingContext;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.SpringAdapterJsonDtoCrudController;

public class ExampleController
        extends SpringAdapterJsonDtoCrudController<ExampleEntity,Long>
{
    public ExampleController(DtoMappingContext dtoMappingContext) {
        super(dtoMappingContext);
    }
}