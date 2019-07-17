package io.github.vincemann.generic.crud.lib.test.controller.springAdapter;

import io.github.vincemann.generic.crud.lib.controller.dtoMapper.EntityMappingException;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.DTOCrudControllerSpringAdapter;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.idFetchingStrategy.UrlParamIdFetchingStrategy;
import io.github.vincemann.generic.crud.lib.model.IdentifiableEntity;
import io.github.vincemann.generic.crud.lib.service.CrudService;
import io.github.vincemann.generic.crud.lib.service.exception.NoIdException;
import io.github.vincemann.generic.crud.lib.test.IntegrationTest;
import io.github.vincemann.generic.crud.lib.test.controller.springAdapter.testBundles.TestDtoBundle;
import io.github.vincemann.generic.crud.lib.test.controller.springAdapter.testBundles.UpdateTestBundle;
import io.github.vincemann.generic.crud.lib.util.BeanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.*;

import static io.github.vincemann.generic.crud.lib.util.BeanUtils.isDeepEqual;

/**
 * Integration Test for a {@link DTOCrudControllerSpringAdapter} with {@link UrlParamIdFetchingStrategy}, that tests typical Crud operation tests
 *
 * @param <ServiceE>
 * @param <DTO>
 * @param <Service>
 * @param <Controller>
 * @param <Id>
 */
public abstract class UrlParamIdDTOCrudControllerSpringAdapterIT<ServiceE extends IdentifiableEntity<Id>, DTO extends IdentifiableEntity<Id>, Service extends CrudService<ServiceE, Id>, Controller extends DTOCrudControllerSpringAdapter<ServiceE, DTO, Id, Service>, Id extends Serializable> extends IntegrationTest {

    /**
     * This is a security feature.
     * If there are more entities in the database than this value, the database wont be cleared after the test, and the test will fail.
     */
    private static final int MAX_AMOUNT_ENTITIES_IN_REPO_WHEN_DELETING_ALL = 200;

    private final Controller crudController;
    private final Class<DTO> dtoEntityClass;
    private final String entityIdParamKey;
    private int safetyCheckMaxAmountEntitiesInRepo = MAX_AMOUNT_ENTITIES_IN_REPO_WHEN_DELETING_ALL;
    private List<TestDtoBundle<DTO>> testDtoBundles;
    private NonExistingIdFinder<Id> nonExistingIdFinder;

    /**
     *
     * @param url
     * @param crudController
     * @param nonExistingId   this can be null if you want to set your own {@link NonExistingIdFinder} with {@link #setNonExistingIdFinder(NonExistingIdFinder)}
     */
    public UrlParamIdDTOCrudControllerSpringAdapterIT(String url, Controller crudController, Id nonExistingId) {
        super(url);
        Assertions.assertTrue(crudController.getIdIdFetchingStrategy() instanceof UrlParamIdFetchingStrategy, "Controller must have UrlParamIdFetchingStrategy");
        this.crudController = crudController;
        this.dtoEntityClass = crudController.getDtoClass();
        this.entityIdParamKey = ((UrlParamIdFetchingStrategy) crudController.getIdIdFetchingStrategy()).getIdUrlParamKey();
        this.nonExistingIdFinder = () -> nonExistingId;
    }

    public UrlParamIdDTOCrudControllerSpringAdapterIT(Controller crudController, Id nonExistingId) {
        super();
        Assertions.assertTrue(crudController.getIdIdFetchingStrategy() instanceof UrlParamIdFetchingStrategy, "Controller must have UrlParamIdFetchingStrategy");
        this.crudController = crudController;
        this.dtoEntityClass = crudController.getDtoClass();
        this.entityIdParamKey = ((UrlParamIdFetchingStrategy) crudController.getIdIdFetchingStrategy()).getIdUrlParamKey();
        this.nonExistingIdFinder = () -> nonExistingId;
    }


    @BeforeEach
    public void before() throws Exception {
        this.testDtoBundles = provideValidTestDTOs();
        Assertions.assertFalse(testDtoBundles.isEmpty());
    }

    /**
     *
     * @return  a list of {@link TestDtoBundle}s with valid {@link TestDtoBundle#getDto()} according to the provided {@link io.github.vincemann.generic.crud.lib.controller.springAdapter.validationStrategy.ValidationStrategy}
     *          These DTO's will be used for all tests in this class
     *          The {@link TestDtoBundle#getUpdateTestBundles()} should have valid modified dto {@link UpdateTestBundle#getModifiedDto()} get used for update tests, that should be successful
     */
    protected abstract List<TestDtoBundle<DTO>> provideValidTestDTOs();

    @Test
    protected void findEntityTest() throws Exception {
        for (TestDtoBundle<DTO> bundle : this.testDtoBundles) {
            System.err.println("findEntityTest with testDTO: " + bundle.getDto());
            DTO savedEntity = createEntityShouldSucceed(bundle.getDto(), HttpStatus.OK);
            DTO responseDTO = findEntityShouldSucceed(savedEntity.getId(), HttpStatus.OK);
            validateDTOsAreDeepEqual(responseDTO, savedEntity);
            bundle.getPostFindCallback().callback(responseDTO);
            System.err.println("Test succeeded");
        }
    }

    @Test
    protected void findNonExistentEntityTest() {
        ResponseEntity<String> responseEntity = findEntity(nonExistingIdFinder.findNonExistingId(), HttpStatus.NOT_FOUND);
        Assertions.assertFalse(isBodyOfDtoType(responseEntity.getBody()));
    }

    @Test
    protected void deleteNonExistentEntityTest() {
        deleteEntity(nonExistingIdFinder.findNonExistingId(), HttpStatus.NOT_FOUND);
    }

    @Test
    protected void updateEntityTest() throws Exception {
        for (TestDtoBundle<DTO> bundle : this.testDtoBundles) {
            System.err.println("updateEntityTest with testDTO: " +  bundle.getDto());

            List<UpdateTestBundle<DTO>> updateTestBundles = bundle.getUpdateTestBundles();
            if(updateTestBundles.isEmpty()){
                System.err.println("no update tests for this entity");
                return;
            }
            for (UpdateTestBundle<DTO> updateTestBundle : updateTestBundles) {
                DTO modifiedDto = updateTestBundle.getModifiedDto();
                System.err.println("update test with modified dto: " + modifiedDto);
                //save old dto
                Assertions.assertNull(bundle.getDto().getId());
                DTO savedDTOEntity = createEntityShouldSucceed(bundle.getDto(), HttpStatus.OK);
                modifiedDto.setId(savedDTOEntity.getId());
                //update dto
                DTO dbUpdatedDto = updateEntityShouldSucceed(savedDTOEntity, modifiedDto, HttpStatus.OK);
                updateTestBundle.getPostUpdateCallback().callback(dbUpdatedDto);
                //remove dto -> clean for next iteration
                deleteExistingEntityShouldSucceed(savedDTOEntity.getId());
                System.err.println("update Test succeeded");
            }

            System.err.println("Test succeeded");
        }
    }

    @Test
    protected void deleteEntityTest() throws Exception {
        for (TestDtoBundle<DTO> bundle : this.testDtoBundles) {
            System.err.println("deleteEntityTest with testDTO: " + bundle.getDto());
            DTO savedEntity = createEntityShouldSucceed(bundle.getDto(), HttpStatus.OK);
            deleteExistingEntityShouldSucceed(savedEntity.getId());
            bundle.getPostDeleteCallback().callback(savedEntity);
            System.err.println("Test succeeded");
        }
    }

    @Test
    protected void createEntityTest() throws Exception {
        for (TestDtoBundle<DTO> bundle : this.testDtoBundles) {
            System.err.println("createEntityTest with testDTO: " + bundle.getDto());
            DTO savedDto = createEntityShouldSucceed(bundle.getDto(), HttpStatus.OK);
            bundle.getPostCreateCallback().callback(savedDto);
            System.err.println("Test succeeded");
        }
    }

    protected ResponseEntity deleteExistingEntityShouldSucceed(Id id) throws NoIdException {
        //Entity muss vorher auch schon da sein
        Optional<ServiceE> serviceFoundEntityBeforeDelete = crudController.getCrudService().findById(id);
        Assertions.assertTrue(serviceFoundEntityBeforeDelete.isPresent(), "Entity to delete was not present");

        ResponseEntity responseEntity = deleteEntity(id);
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode()+" response Body: " + responseEntity.getBody());

        //is it really deleted?
        Optional<ServiceE> serviceFoundEntity = crudController.getCrudService().findById(id);
        Assertions.assertFalse(serviceFoundEntity.isPresent());
        return responseEntity;
    }

    protected ResponseEntity deleteExistingEntityShouldFail(Id id) throws NoIdException {
        //Entity muss vorher auch schon da sein
        Optional<ServiceE> serviceFoundEntityBeforeDelete = crudController.getCrudService().findById(id);
        Assertions.assertTrue(serviceFoundEntityBeforeDelete.isPresent(), "Entity to delete was not present");

        ResponseEntity responseEntity = deleteEntity(id);
        Assertions.assertFalse(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode()+" response Body: " + responseEntity.getBody());

        //is it really not deleted?
        Optional<ServiceE> serviceFoundEntity = crudController.getCrudService().findById(id);
        Assertions.assertTrue(serviceFoundEntity.isPresent());
        return responseEntity;
    }

    protected ResponseEntity deleteEntity(Id id) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + crudController.getDeleteMethodName())
                .queryParam(entityIdParamKey, id);
        RequestEntity requestEntity = new RequestEntity(HttpMethod.DELETE, builder.build().toUri());
        return getRestTemplate().exchange(requestEntity, Object.class);
    }

    protected ResponseEntity deleteEntity(Id id, HttpStatus httpStatus) {
        ResponseEntity responseEntity = deleteEntity(id);
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode(), "Status was : " + responseEntity.getStatusCode()+" response Body: " + responseEntity.getBody());
        return responseEntity;
    }

    /**
     * send find Entity Request to backend
     * expect {@link HttpStatus} status code to be 2xx
     * expect status code to be specified {@link HttpStatus} statuscode
     * parse Body to {@link DTO} dtoObject
     *
     * @param id
     * @return parsed {@link DTO} dtoObject
     * @throws Exception
     */
    protected DTO findEntityShouldSucceed(Id id, HttpStatus httpStatus) throws Exception {
        ResponseEntity<String> responseEntity = findEntity(id);
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode()+" response Body: " + responseEntity.getBody());
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode());
        DTO httpResponseEntity = crudController.getMediaTypeStrategy().readDTOFromBody(responseEntity.getBody(), dtoEntityClass);
        Assertions.assertNotNull(httpResponseEntity);
        Assertions.assertTrue(isSavedServiceEntityDeepEqual(httpResponseEntity));
        return httpResponseEntity;
    }

    /*protected ResponseEntity findEntityShouldFail(Id id) {
        ResponseEntity responseEntity = findEntity(id);
        Assertions.assertFalse(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode());
        return responseEntity;
    }*/

    /**
     * send find Entity Request to backend
     *
     * @param id
     * @return backend Response
     */
    protected ResponseEntity<String> findEntity(Id id) {
        Assertions.assertNotNull(id);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + crudController.getFindMethodName())
                .queryParam(entityIdParamKey, id);
        return getRestTemplate().getForEntity(builder.build().toUriString(), String.class);
    }

    /**
     * send find Entity Request to backend and expect specified {@link HttpStatus} status code
     *
     * @param id
     * @param httpStatus
     * @return backend Response
     */
    protected ResponseEntity<String> findEntity(Id id, HttpStatus httpStatus) {
        ResponseEntity<String> responseEntity = findEntity(id);
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode(), "Status was : " + responseEntity.getStatusCode()+" response Body: " + responseEntity.getBody());
        return responseEntity;
    }


    /**
     * 1. Send create Entity Request to Backend
     * 2. Expect 2xx {@link HttpStatus} statuscode from backend
     * 3. Expect the specified {@link HttpStatus}  statuscode from backend
     * 4. assert returned DTO entity is deep equal to ServiceEntity via  {@link #isSavedServiceEntityDeepEqual(IdentifiableEntity)}
     *
     * @param dtoEntity
     * @return
     * @throws Exception
     */
    protected DTO createEntityShouldSucceed(DTO dtoEntity, HttpStatus httpStatus) throws Exception {
        Assertions.assertNull(dtoEntity.getId());
        ResponseEntity<String> responseEntity = createEntity(dtoEntity);
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode() + " response Body: " + responseEntity.getBody());
        Assertions.assertEquals(responseEntity.getStatusCode(), httpStatus);
        DTO httpResponseEntity = crudController.getMediaTypeStrategy().readDTOFromBody(responseEntity.getBody(), dtoEntityClass);
        Assertions.assertTrue(isSavedServiceEntityDeepEqual(httpResponseEntity));
        return httpResponseEntity;
    }

    /**
     * Send create Entity Request to Backend, Response is returned
     *
     * @param dtoEntity the DTO entity that should be stored
     * @return
     */
    protected ResponseEntity<String> createEntity(DTO dtoEntity) {
        return getRestTemplate().postForEntity(getBaseUrl() + crudController.getCreateMethodName(), dtoEntity, String.class);
    }

    /**
     * Same as {@link #createEntity(IdentifiableEntity dtoEntity)} but the specified {@link HttpStatus} must be returned by Backend
     *
     * @param dtoEntity  the DTO entity that should be stored
     * @param httpStatus the expected http Status
     * @return
     */
    protected ResponseEntity<String> createEntity(DTO dtoEntity, HttpStatus httpStatus) {
        ResponseEntity<String> responseEntity = createEntity(dtoEntity);
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode());
        return responseEntity;
    }

    /**
     * 1. expect oldEntityDTO and newEntityDTO to not be deepEqual {@link BeanUtils#isDeepEqual(Object, Object)}
     * 2. expect oldEntityDTO and newEntityDTO to have same id
     * 3. expect oldEntityDTO to be already persisted -> can be found by id
     * 4. make update request to backend
     * 5. expect {@link HttpStatus} response status code to be 2xx
     * 6. expect {@link HttpStatus} response status code to be specified {@link HttpStatus} statuscode
     * 7. returned EntityDTO by backend must be deepEqual to newEntityDTO and persisted entityDTO with the id of newEntityDTO
     * 8. oldEntityDTO must differ from returned EntityDTO by backend
     *
     * @param oldEntityDTO entityDTO already saved that should be updated
     * @param newEntityDTO entityDTO that should replace/update old entity
     * @return updated entityDTO returned by backend
     * @throws Exception
     */
    protected DTO updateEntityShouldSucceed(DTO oldEntityDTO, DTO newEntityDTO, HttpStatus httpStatus) throws Exception {
        Assertions.assertNotNull(oldEntityDTO.getId());
        Assertions.assertNotNull(newEntityDTO.getId());
        Assertions.assertEquals(oldEntityDTO.getId(), newEntityDTO.getId());
        //Entity muss vorher auch schon da sein
        Optional<ServiceE> serviceFoundEntityBeforeUpdate = crudController.getCrudService().findById(newEntityDTO.getId());
        Assertions.assertTrue(serviceFoundEntityBeforeUpdate.isPresent(), "Entity to delete was not present");
        //trotzdem müssen changes vorliegen
        Assertions.assertFalse(isDeepEqual(oldEntityDTO, newEntityDTO));

        ResponseEntity<String> responseEntity = updateEntity(newEntityDTO);
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode());
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode());
        DTO httpResponseDTO = crudController.getMediaTypeStrategy().readDTOFromBody(responseEntity.getBody(), dtoEntityClass);
        Assertions.assertNotNull(httpResponseDTO);
        //response http entity must match modTestEntity
        validateDTOsAreDeepEqual(httpResponseDTO, newEntityDTO);
        //entity fetched from vincemann.github.generic.crud.lib.service by id must match httpResponseEntity
        Assertions.assertTrue(isSavedServiceEntityDeepEqual(httpResponseDTO));
        //entity fetched from vincemann.github.generic.crud.lib.service at start of test (before update) must not match httpResponseEntity (since it got updated)
        boolean deepEqual = isDeepEqual(oldEntityDTO, httpResponseDTO);
        Assertions.assertFalse(deepEqual, "Entites did match but must not -> entity was not updated");
        return httpResponseDTO;
    }

    protected void updateEntityShouldFail(DTO oldEntity, DTO newEntity, HttpStatus httpStatus) throws Exception {
        Assertions.assertNotNull(oldEntity.getId());
        Assertions.assertNotNull(newEntity.getId());
        Assertions.assertEquals(oldEntity.getId(), newEntity.getId());
        //Entity muss vorher auch schon da sein
        Optional<ServiceE> serviceFoundEntityBeforeUpdate = crudController.getCrudService().findById(newEntity.getId());
        Assertions.assertTrue(serviceFoundEntityBeforeUpdate.isPresent(), "Entity to delete was not present");
        //id muss gleich sein

        //trotzdem müssen changes vorliegen
        Assertions.assertFalse(isDeepEqual(oldEntity, newEntity));

        ResponseEntity<String> responseEntity = updateEntity(newEntity);
        Assertions.assertFalse(responseEntity.getStatusCode().is2xxSuccessful(), "Status was : " + responseEntity.getStatusCode());
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode());

        //entity aus Service muss immernoch die gleiche sein wie vorher
        Assertions.assertTrue(isSavedServiceEntityDeepEqual(oldEntity));
    }

    /**
     * Send update Entity Request to Backend
     *
     * @param newEntity updated entityDTO
     * @return backend Response
     */
    private ResponseEntity<String> updateEntity(DTO newEntity) {
        Assertions.assertNotNull(newEntity.getId());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + crudController.getUpdateMethodName())
                .queryParam(entityIdParamKey, newEntity.getId());

        RequestEntity<DTO> requestEntity = new RequestEntity<DTO>(newEntity, HttpMethod.PUT, builder.build().toUri());
        return getRestTemplate().exchange(requestEntity, String.class);
    }

    /**
     * Send update Entity Request to Backend and expect specified {@link HttpStatus} status code
     *
     * @param newEntity  updated entityDTO
     * @param httpStatus expected status code
     * @return backend Response
     */
    protected ResponseEntity<String> updateEntity(DTO newEntity, HttpStatus httpStatus) {
        Assertions.assertNotNull(newEntity.getId());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + crudController.getUpdateMethodName())
                .queryParam(entityIdParamKey, newEntity.getId());

        RequestEntity<DTO> requestEntity = new RequestEntity<DTO>(newEntity, HttpMethod.PUT, builder.build().toUri());
        ResponseEntity<String> responseEntity = getRestTemplate().exchange(requestEntity, String.class);
        Assertions.assertEquals(httpStatus, responseEntity.getStatusCode());
        return responseEntity;
    }


    /**
     * 1. Map DTOEntity to ServiceEntity = RequestServiceEntity
     * 2. Fetch ServiceEntity from Service (ultimately from the persistence layer) by Id = dbServiceEntity
     * 3. Validate that RequestServiceEntity and dbServiceEntity are deep equal via {@link BeanUtils#isDeepEqual(Object, Object)}
     *
     * @param httpResponseEntity the DTO entity returned by Backend after http request
     * @return
     * @throws NoIdException
     */
    protected boolean isSavedServiceEntityDeepEqual(DTO httpResponseEntity) throws EntityMappingException{
        try {
            ServiceE serviceHttpResponseEntity = crudController.getDtoMapper().mapDtoToServiceEntity(httpResponseEntity,crudController.getServiceEntityClass());
            Assertions.assertNotNull(serviceHttpResponseEntity);
            Id httpResponseEntityId = serviceHttpResponseEntity.getId();
            Assertions.assertNotNull(httpResponseEntityId);

            //Compare httpEntity with saved Entity From Service
            Optional<ServiceE> entityFromService = crudController.getCrudService().findById(httpResponseEntityId);
            Assertions.assertTrue(entityFromService.isPresent());
            return isDeepEqual(entityFromService.get(), serviceHttpResponseEntity);
        } catch (NoIdException e) {
            throw new EntityMappingException(e);
        }

    }

    /**
     * see {@link BeanUtils#isDeepEqual(Object, Object)}
     *
     * @param httpResponseEntity
     * @param prevSavedEntity
     */
    protected void validateDTOsAreDeepEqual(DTO httpResponseEntity, DTO prevSavedEntity) {
        boolean deepEqual = areDTOsDeepEqual(httpResponseEntity, prevSavedEntity);
        Assertions.assertTrue(deepEqual, "Entities did not match");
    }

    /**
     * see {@link BeanUtils#isDeepEqual(Object, Object)}
     *
     * @param httpResponseEntity
     * @param prevSavedEntity
     * @return
     */
    protected boolean areDTOsDeepEqual(DTO httpResponseEntity, DTO prevSavedEntity) {
        return isDeepEqual(httpResponseEntity, prevSavedEntity);
    }


    /**
     * removes all Entites from given {@link Service}
     * checks whether specified max amount of entities is exeeded : {@link UrlParamIdDTOCrudControllerSpringAdapterIT#setSafetyCheckMaxAmountEntitiesInRepo(int)}
     */
    @AfterEach
    public void tearDown() throws Exception {
        Set<ServiceE> allEntities = crudController.getCrudService().findAll();
        if (allEntities.size() >= safetyCheckMaxAmountEntitiesInRepo) {
            throw new RuntimeException("max amount of entities in repo exceeded, tried to delete " + allEntities.size() + " entites. Do you have the wrong datasource?");
        }
        for (ServiceE entityToDelete : allEntities) {
            crudController.getCrudService().deleteById(entityToDelete.getId());
        }
        Set<ServiceE> allEntitiesAfterDeleting = crudController.getCrudService().findAll();
        Assertions.assertTrue(allEntitiesAfterDeleting.isEmpty());
    }

    protected boolean isBodyOfDtoType(String body) {
        return getCrudController().getMediaTypeStrategy().isBodyOfGivenType(body, getDtoEntityClass());
    }

    /**
     * use with caution
     *
     * @param safetyCheckMaxAmountEntitiesInRepo
     */
    protected void setSafetyCheckMaxAmountEntitiesInRepo(int safetyCheckMaxAmountEntitiesInRepo) {
        this.safetyCheckMaxAmountEntitiesInRepo = safetyCheckMaxAmountEntitiesInRepo;
    }

    private String getBaseUrl() {
        return getUrlWithPort() + "/" + crudController.getEntityNameInUrl() + "/";
    }

    protected Controller getCrudController() {
        return crudController;
    }


    public Class<DTO> getDtoEntityClass() {
        return dtoEntityClass;
    }

    public List<TestDtoBundle<DTO>> getTestDtoBundles() {
        return testDtoBundles;
    }

    public void setNonExistingIdFinder(NonExistingIdFinder<Id> nonExistingIdFinder) {
        this.nonExistingIdFinder = nonExistingIdFinder;
    }

    public NonExistingIdFinder<Id> getNonExistingIdFinder() {
        return nonExistingIdFinder;
    }
}