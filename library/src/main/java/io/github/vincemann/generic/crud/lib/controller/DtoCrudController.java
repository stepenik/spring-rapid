package io.github.vincemann.generic.crud.lib.controller;

import io.github.vincemann.generic.crud.lib.controller.dtoMapper.exception.EntityMappingException;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.DtoSerializingException;
import io.github.vincemann.generic.crud.lib.model.IdentifiableEntity;
import io.github.vincemann.generic.crud.lib.service.exception.BadEntityException;
import io.github.vincemann.generic.crud.lib.service.exception.EntityNotFoundException;
import io.github.vincemann.generic.crud.lib.service.exception.NoIdException;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;


/**
 * Defines Crud Operations a DtoCrudController must implement
 * @param <Id>      Id Type of corresponding Service Entity
 */
public interface DtoCrudController<Id extends Serializable> {

    ResponseEntity<String> create(IdentifiableEntity<Id> entity) throws BadEntityException, EntityMappingException, DtoSerializingException;

    ResponseEntity<String> find(Id id) throws NoIdException, EntityNotFoundException, EntityMappingException, DtoSerializingException;

    /**
     *
     * @param entity
     * @param full      indicates whether all set values should be recognized as new values or only non null values
     *                  If you want to delete values (aka setting them null) with the update, then set this flag to true.
     * @return
     * @throws EntityMappingException
     * @throws BadEntityException
     * @throws NoIdException
     * @throws EntityNotFoundException
     */
    ResponseEntity<String> update(IdentifiableEntity<Id> entity, boolean full) throws EntityMappingException, BadEntityException, NoIdException, EntityNotFoundException, DtoSerializingException;

    ResponseEntity<String> delete(Id id) throws NoIdException, EntityNotFoundException;

    ResponseEntity<String> findAll() throws EntityMappingException, DtoSerializingException;
}
