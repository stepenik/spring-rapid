package io.github.vincemann.springrapid.demo.controllers;

import io.github.vincemann.springrapid.demo.dtos.VetDto;
import io.github.vincemann.springrapid.demo.model.Vet;
import io.github.vincemann.springrapid.core.slicing.components.WebController;
import io.github.vincemann.springrapid.core.controller.dtoMapper.context.DtoMappingContextBuilder;
import io.github.vincemann.springrapid.core.controller.rapid.RapidController;
import io.github.vincemann.springrapid.demo.service.VetService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@WebController
public class VetController
        extends RapidController<Vet, Long, VetService> {

    public VetController() {
        super(DtoMappingContextBuilder.builder()
                .forAll(VetDto.class)
                .build()
        );
    }

}
