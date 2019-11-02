package io.github.vincemann.generic.crud.lib.controller.springAdapter;

import io.github.vincemann.generic.crud.lib.controller.dtoMapper.EntityMappingException;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.idFetchingStrategy.exception.IdFetchingException;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.idFetchingStrategy.IdFetchingStrategy;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.mediaTypeStrategy.DtoReadingException;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.mediaTypeStrategy.MediaTypeStrategy;
import io.github.vincemann.generic.crud.lib.controller.springAdapter.validationStrategy.ValidationStrategy;
import io.github.vincemann.generic.crud.lib.controller.dtoMapper.DtoMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import io.github.vincemann.generic.crud.lib.controller.BasicDtoCrudController;
import io.github.vincemann.generic.crud.lib.model.IdentifiableEntity;
import io.github.vincemann.generic.crud.lib.service.CrudService;
import io.github.vincemann.generic.crud.lib.service.EndpointService;
import io.github.vincemann.generic.crud.lib.service.exception.BadEntityException;
import io.github.vincemann.generic.crud.lib.service.exception.EntityNotFoundException;
import io.github.vincemann.generic.crud.lib.service.exception.NoIdException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter that connects Springs Requirements for a Controller (which can be seen as an Interface),
 * with the {@link BasicDtoCrudController} Interface, resulting in a fully functional Spring @{@link org.springframework.web.bind.annotation.RestController}
 *
 * fetches {@link Id} with given {@link IdFetchingStrategy} from HttpRequest
 * fetches {@link Dto} with given {@link MediaTypeStrategy} from HttpRequest
 * validates the {@link Dto} and {@link Id} with the given {@link ValidationStrategy}
 *
 * ExampleUrls with {@link io.github.vincemann.generic.crud.lib.controller.springAdapter.idFetchingStrategy.UrlParamIdFetchingStrategy}:
 * /entityName/httpMethod?entityIdName=id
 *
 * /account/get?accountId=34
 * /account/get?accountId=44bedc08-8e71-11e9-bc42-526af7764f64
 *
 * @param <ServiceE> Service Entity Type, of entity, which's curd operations are exposed, via endpoints,  by this Controller
 * @param <Service>  Service Type of {@link CrudService} managing {@link ServiceE}s
 * @param <Dto>      Dto Type corresponding to {@link ServiceE}
 * @param <Id>       Id Type of {@link ServiceE}
 *
 */
@Slf4j
public abstract class DtoCrudControllerSpringAdapter<ServiceE extends IdentifiableEntity<Id>,Dto extends IdentifiableEntity<Id>,Id extends Serializable, Service extends CrudService<ServiceE,Id>>
        extends BasicDtoCrudController<ServiceE,Dto,Id,Service> {


    private EndpointService endpointService;
    private String entityNameInUrl;
    @Getter
    private String baseUrl;
    private String findMethodName ="get";
    private String createMethodName="create";
    private String deleteMethodName="delete";
    private String updateMethodName="update";
    private String findAllMethodName = "getAll";

    private String findUrl;
    private String updateUrl;
    private String getAllUrl;
    private String deleteUrl;
    private String createUrl;

    private IdFetchingStrategy<Id> idIdFetchingStrategy;
    private MediaTypeStrategy mediaTypeStrategy;
    private ValidationStrategy<? super Dto,? super Id> validationStrategy;
    private EndpointsExposureDetails endpointsExposureDetails;
    private List<Plugin<? super ServiceE,? super Dto,? super Id>> plugins = new ArrayList<>();

    //todo implement methods, that only return id, and not whole dtos
    public DtoCrudControllerSpringAdapter(Service crudService,
                                          EndpointService endpointService,
                                          String entityNameInUrl,
                                          IdFetchingStrategy<Id> idIdFetchingStrategy,
                                          MediaTypeStrategy mediaTypeStrategy,
                                          ValidationStrategy<? super Dto,? super Id> validationStrategy,
                                          DtoMapper dtoMapper,
                                          EndpointsExposureDetails endpointsExposureDetails,
                                          Plugin<? super ServiceE,? super Dto,? super Id>... plugins
    ) {
        super(crudService, dtoMapper);
        constructorInit(endpointService,entityNameInUrl,idIdFetchingStrategy,mediaTypeStrategy,validationStrategy,endpointsExposureDetails,plugins);
    }

    public DtoCrudControllerSpringAdapter(Service crudService,
                                          EndpointService endpointService,
                                          IdFetchingStrategy<Id> idIdFetchingStrategy,
                                          MediaTypeStrategy mediaTypeStrategy,
                                          ValidationStrategy<? super Dto,? super Id> validationStrategy,
                                          DtoMapper dtoMapper,
                                          EndpointsExposureDetails endpointsExposureDetails,
                                          Plugin<? super ServiceE,? super Dto,? super Id>... plugins
    ) {
        super(crudService, dtoMapper);
        constructorInit(endpointService,getServiceEntityClass().getSimpleName().toLowerCase(),idIdFetchingStrategy,mediaTypeStrategy,validationStrategy,endpointsExposureDetails,plugins);
    }

    private void constructorInit(EndpointService endpointService, String entityNameInUrl, IdFetchingStrategy<Id> idIdFetchingStrategy, MediaTypeStrategy mediaTypeStrategy, ValidationStrategy<? super Dto,? super Id> validationStrategy, EndpointsExposureDetails endpointsExposureDetails, Plugin<? super ServiceE,? super Dto,? super Id>... plugins){
        this.endpointService = endpointService;
        this.entityNameInUrl = entityNameInUrl;
        this.baseUrl="/"+entityNameInUrl+"/";
        this.idIdFetchingStrategy=idIdFetchingStrategy;
        this.mediaTypeStrategy=mediaTypeStrategy;
        this.validationStrategy = validationStrategy;
        this.endpointsExposureDetails = endpointsExposureDetails;
        initUrls();
        initRequestMapping();
        this.initPlugins(plugins);
    }

    private void initUrls(){
        this.findUrl =baseUrl+getFindMethodName();
        this.getAllUrl=baseUrl+getFindAllMethodName();
        this.updateUrl=baseUrl+getUpdateMethodName();
        this.deleteUrl=baseUrl+getDeleteMethodName();
        this.createUrl=baseUrl+getCreateMethodName();
    }

    private void initPlugins(Plugin<? super ServiceE,? super Dto,? super Id>... crudControllerSpringAdapterPlugins){
        List<Plugin<? super ServiceE, ? super Dto, ? super Id>> plugins = Arrays.asList(crudControllerSpringAdapterPlugins);
        plugins.forEach(plugin -> {
            log.debug("Initializing plugin: "+ plugin + " for controller: "+this.getClass().getSimpleName());
            plugin.setController(this);

        });
        this.plugins.addAll(plugins);
        this.getBasicCrudControllerPlugins().addAll(plugins);
    }


    private void initRequestMapping(){
        try {
            if(endpointsExposureDetails.isCreateEndpointExposed()) {
                //CREATE
                log.debug("Exposing create Endpoint for "+this.getClass().getSimpleName());
                getEndpointService().addMapping(getCreateRequestMappingInfo(),
                        this.getClass().getMethod("create", HttpServletRequest.class), this);
            }

            if(endpointsExposureDetails.isFindEndpointExposed()) {
                //GET
                log.debug("Exposing get Endpoint for "+this.getClass().getSimpleName());
                getEndpointService().addMapping(getFindRequestMappingInfo(),
                        this.getClass().getMethod("find", HttpServletRequest.class), this);
            }

            if(endpointsExposureDetails.isUpdateEndpointExposed()) {
                //UPDATE
                log.debug("Exposing update Endpoint for "+this.getClass().getSimpleName());
                getEndpointService().addMapping(getUpdateRequestMappingInfo(),
                        this.getClass().getMethod("update", HttpServletRequest.class), this);
            }

            if(endpointsExposureDetails.isDeleteEndpointExposed()) {
                //DELETE
                log.debug("Exposing delete Endpoint for "+this.getClass().getSimpleName());
                getEndpointService().addMapping(getDeleteRequestMappingInfo(),
                        this.getClass().getMethod("delete", HttpServletRequest.class), this);
            }

            if(endpointsExposureDetails.isFindAllEndpointExposed()){
                //DELETE
                log.debug("Exposing delete Endpoint for "+this.getClass().getSimpleName());
                getEndpointService().addMapping(getFindAllRequestMappingInfo(),
                        this.getClass().getMethod("findAll", HttpServletRequest.class), this);
            }

        }catch (NoSuchMethodException e){
            //should never happen
            throw new IllegalStateException(e);
        }
    }

    public RequestMappingInfo getFindRequestMappingInfo(){
        return RequestMappingInfo
                .paths(findUrl)
                .methods(RequestMethod.GET)
                .produces(mediaTypeStrategy.getMediaType())
                .build();
    }

    public RequestMappingInfo getDeleteRequestMappingInfo(){
        return RequestMappingInfo
                .paths(deleteUrl)
                .methods(RequestMethod.DELETE)
                .build();
    }

    public RequestMappingInfo getCreateRequestMappingInfo(){
        return RequestMappingInfo
                .paths(createUrl)
                .methods(RequestMethod.POST)
                .consumes(mediaTypeStrategy.getMediaType())
                .produces(mediaTypeStrategy.getMediaType())
                .build();
    }

    public RequestMappingInfo getUpdateRequestMappingInfo(){
        return RequestMappingInfo
                .paths(updateUrl)
                .methods(RequestMethod.PUT)
                .consumes(mediaTypeStrategy.getMediaType())
                .produces(mediaTypeStrategy.getMediaType())
                .build();
    }

    public RequestMappingInfo getFindAllRequestMappingInfo(){
        return RequestMappingInfo
                .paths(getAllUrl)
                .methods(RequestMethod.GET)
                .produces(mediaTypeStrategy.getMediaType())
                .build();
    }

    public ResponseEntity<Collection<Dto>> findAll(HttpServletRequest request) throws EntityMappingException {
        log.debug("FindAll request arriving at controller: " + request);
        validationStrategy.validateFindAllRequest(request);
        log.debug("Find all request validated");
        beforeFindAll(request);
        return super.findAll();
    }


    public ResponseEntity<Dto> find(HttpServletRequest request) throws IdFetchingException, EntityNotFoundException, NoIdException, EntityMappingException {
        log.debug("Find request arriving at controller: " + request);
        Id id = idIdFetchingStrategy.fetchId(request);
        validationStrategy.beforeFindValidate(id);
        log.debug("id fetched from request: " + id);
        validationStrategy.validateId(id,request);
        log.debug("id successfully validated");
        beforeFind(id,request);
        return super.find(id);
    }

    public ResponseEntity<Dto> create(HttpServletRequest request) throws DtoReadingException, BadEntityException, EntityMappingException {
        log.debug("Create request arriving at controller: " + request);
        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            log.debug("String Body fetched from request: " + body);
            Dto dto = mediaTypeStrategy.readDtoFromBody(body,getDtoClass());
            log.debug("Dto read from string body " + dto);
            validationStrategy.beforeCreateValidate(dto);
            validationStrategy.validateDto(dto,request);
            log.debug("Dto successfully validated");
            beforeCreate(dto,request);
            return super.create(dto);
        }catch (IOException e){
            throw new DtoReadingException(e);
        }
    }

    public ResponseEntity<Dto> update(HttpServletRequest request) throws DtoReadingException, EntityNotFoundException, NoIdException, BadEntityException, EntityMappingException {
        log.debug("Update request arriving at controller: " + request);
        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            log.debug("String Body fetched from request: " + body);
            Dto dto = mediaTypeStrategy.readDtoFromBody(body,getDtoClass());
            log.debug("Dto read from string body " + dto);
            validationStrategy.beforeUpdateValidate(dto);
            validationStrategy.validateDto(dto,request);
            log.debug("Dto successfully validated");
            beforeUpdate(dto,request);
            return super.update(dto);
        }catch (IOException e){
            throw new DtoReadingException(e);
        }
    }


    public ResponseEntity delete(HttpServletRequest request) throws IdFetchingException, NoIdException, EntityNotFoundException, ConstraintViolationException {
        log.debug("Delete request arriving at controller: " + request);
        Id id = idIdFetchingStrategy.fetchId(request);
        log.debug("id fetched from request: " + id);
        validationStrategy.beforeDeleteValidate(id);
        validationStrategy.validateId(id,request);
        log.debug("id successfully validated");
        beforeDelete(id,request);
        return super.delete(id);
    }

    protected void beforeCreate(Dto dto, HttpServletRequest httpServletRequest){
        plugins.forEach(plugin -> plugin.beforeCreate(dto,httpServletRequest));
    }
    protected void beforeUpdate(Dto dto, HttpServletRequest httpServletRequest){
        plugins.forEach(plugin -> plugin.beforeUpdate(dto,httpServletRequest));
    }
    protected void beforeDelete(Id id, HttpServletRequest httpServletRequest){
        plugins.forEach(plugin -> plugin.beforeDelete(id,httpServletRequest));
    }
    protected void beforeFind(Id id, HttpServletRequest httpServletRequest){
        plugins.forEach(plugin -> plugin.beforeFind(id,httpServletRequest));
    }
    protected void beforeFindAll(HttpServletRequest httpServletRequest){
        plugins.forEach(plugin -> plugin.beforeFindAll(httpServletRequest));
    }


    @Getter
    @Setter
    public static abstract class Plugin<ServiceE extends IdentifiableEntity<Id>,Dto extends IdentifiableEntity<Id>, Id extends Serializable> implements BasicDtoCrudController.Plugin<ServiceE,Id> {

        private DtoCrudControllerSpringAdapter controller;

        public void beforeCreate(Dto dto, HttpServletRequest httpServletRequest){}
        public void beforeUpdate(Dto dto, HttpServletRequest httpServletRequest){}
        public void beforeDelete(Id id, HttpServletRequest httpServletRequest){}
        public void beforeFind(Id id, HttpServletRequest httpServletRequest){}
        public void beforeFindAll(HttpServletRequest httpServletRequest){}

        @Override
        public void beforeFindEntity(Id id) { }
        @Override
        public void afterFindEntity(ServiceE foundEntity) { }
        @Override
        public void beforeCreateEntity(ServiceE entity) { }
        @Override
        public void afterCreateEntity(ServiceE entity) { }
        @Override
        public void beforeUpdateEntity(ServiceE entity) { }
        @Override
        public void afterUpdateEntity(ServiceE entity) { }
        @Override
        public void beforeDeleteEntity(Id id) { }
        @Override
        public void afterDeleteEntity(Id id) { }
        @Override
        public void beforeFindAllEntities() { }
        @Override
        public void afterFindAllEntities(Set<? extends ServiceE> all) { }
    }



    /*private Long getEntityIdFromPathVar(String url) throws MissingIdException {
        Path urlPath = Paths.find(url);
        String entityId=null;
        int index=0;
        for (Path path: urlPath){
            if(path.toString().equals(entityNameInUrl)){
                if(urlPath.getNameCount()>=index+1) {
                    entityId = urlPath.getName(index + 1).toString();
                }else {
                    throw new MissingIdException("Could not retrieve PathVar " + entityIdParamKey + " from url");
                }
                break;
            }
        }

        if(entityId==null){
            throw new MissingIdException("Could not retrieve PathVar " + entityIdParamKey + " from url");
        }
        try {
            return Long.parseLong(entityId);
        }catch (NumberFormatException e){
            throw new MissingIdException("PathVar " + entityIdParamKey + " is not of Type Long",e);
        }

    }*/

    protected EndpointService getEndpointService() {
        return endpointService;
    }


    public String getFindUrl() {
        return findUrl;
    }

    public void setFindUrl(String findUrl) {
        this.findUrl = findUrl;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getGetAllUrl() {
        return getAllUrl;
    }

    public void setGetAllUrl(String getAllUrl) {
        this.getAllUrl = getAllUrl;
    }

    public String getDeleteUrl() {
        return deleteUrl;
    }

    public void setDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    public String getCreateUrl() {
        return createUrl;
    }

    public void setCreateUrl(String createUrl) {
        this.createUrl = createUrl;
    }

    public String getEntityNameInUrl() {
        return entityNameInUrl;
    }

    public String getFindMethodName() {
        return findMethodName;
    }

    public String getCreateMethodName() {
        return createMethodName;
    }

    public String getDeleteMethodName() {
        return deleteMethodName;
    }

    public String getUpdateMethodName() {
        return updateMethodName;
    }

    public String getFindAllMethodName() {
        return findAllMethodName;
    }

    public void setFindAllMethodName(String findAllMethodName) {
        this.findAllMethodName = findAllMethodName;
    }

    public void setFindMethodName(String findMethodName) {
        this.findMethodName = findMethodName;
    }

    public void setCreateMethodName(String createMethodName) {
        this.createMethodName = createMethodName;
    }

    public void setDeleteMethodName(String deleteMethodName) {
        this.deleteMethodName = deleteMethodName;
    }

    public void setUpdateMethodName(String updateMethodName) {
        this.updateMethodName = updateMethodName;
    }

    public IdFetchingStrategy<Id> getIdIdFetchingStrategy() {
        return idIdFetchingStrategy;
    }

    public MediaTypeStrategy getMediaTypeStrategy() {
        return mediaTypeStrategy;
    }

    public ValidationStrategy<? super Dto,? super Id> getValidationStrategy() {
        return validationStrategy;
    }

    public EndpointsExposureDetails getEndpointsExposureDetails() {
        return endpointsExposureDetails;
    }
}
