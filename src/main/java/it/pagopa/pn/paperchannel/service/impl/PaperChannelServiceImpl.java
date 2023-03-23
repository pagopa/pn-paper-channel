package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import it.pagopa.pn.paperchannel.validator.CostValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;


@Slf4j
@Service
public class PaperChannelServiceImpl implements PaperChannelService {
    @Autowired
    private CostDAO costDAO;
    @Autowired
    private DeliveryDriverDAO deliveryDriverDAO;
    @Autowired
    private TenderDAO tenderDAO;
    @Autowired
    private ExcelDAO<DeliveriesData> excelDAO;
    @Autowired
    private FileDownloadDAO fileDownloadDAO;
    @Autowired
    private PnAuditLogBuilder pnAuditLogBuilder;
    private final S3Bucket s3Bucket;

    public PaperChannelServiceImpl(S3Bucket s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    @Override
    public Mono<PageableTenderResponseDto> getAllTender(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page-1, size);

        return tenderDAO.getTenders()
                .map(list -> TenderMapper.toPagination(pageable, list))
                .map(TenderMapper::toPageableResponse);
    }

    @Override
    public Mono<TenderDetailResponseDTO> getTenderDetails(String tenderCode) {
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(TENDER_NOT_EXISTED, TENDER_NOT_EXISTED.getMessage())))
                .map(tender -> {
                    TenderDetailResponseDTO response = new TenderDetailResponseDTO();
                    response.setCode(TenderDetailResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    response.setTender(TenderMapper.tenderToDto(tender));
                    return response;
                });
    }

    @Override
    public Mono<DeliveryDriverResponseDTO> getDriverDetails(String tenderCode, String driverCode) {
        return this.deliveryDriverDAO.getDeliveryDriver(tenderCode, driverCode)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage(), HttpStatus.NOT_FOUND)))
                .map(driverEntity -> {
                    DeliveryDriverResponseDTO response = new DeliveryDriverResponseDTO();
                    response.setCode(DeliveryDriverResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    response.setDriver(DeliveryDriverMapper.deliveryDriverToDto(driverEntity));
                    return response;
                });
    }

    @Override
    public Mono<FSUResponseDTO> getDetailsFSU(String tenderCode) {
        return this.deliveryDriverDAO.getDeliveryDriverFSU(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage(), HttpStatus.NOT_FOUND)))
                .map(fsu -> {
                    FSUResponseDTO response = new FSUResponseDTO();
                    response.setCode(FSUResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    response.setFsu(DeliveryDriverMapper.deliveryDriverToDto(fsu));
                    return response;
                });
    }

    @Override
    public Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size, Boolean fsu) {
        Pageable pageable = PageRequest.of(page-1, size);
        return deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, fsu)
                .collectList()
                .map(list ->
                        DeliveryDriverMapper.toPagination(pageable, list)
                )
                .map(DeliveryDriverMapper::toPageableResponse);
    }

    @Override
    public Mono<PageableCostResponseDto> getAllCostFromTenderAndDriver(String tenderCode, String driverCode, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page-1, size);
        return costDAO.findAllFromTenderCode(tenderCode, driverCode)
                .collectList()
                .map(list ->
                        CostMapper.toPagination(pageable, list)
                )
                .map(CostMapper::toPageableResponse);
    }

    @Override
    public Mono<PresignedUrlResponseDto> getPresignedUrl() {
        return s3Bucket.presignedUrl()
                .flatMap(presignedUrlResponseDto ->
                        fileDownloadDAO.create(PresignedUrlResponseMapper.toEntity(presignedUrlResponseDto))
                                .map(pnDeliveryFile -> presignedUrlResponseDto)
                );
    }

    public Mono<InfoDownloadDTO> downloadTenderFile(String tenderCode,String uuid) {
        log.info("Start downloadTenderFile");

        if(StringUtils.isNotEmpty(uuid)) {
            return fileDownloadDAO.getUuid(uuid)
                    .map(item -> {
                        byte[] result = null;
                        try {
                            result = s3Bucket.getObjectData(item.getFilename());
                        } catch (Exception e) {
                            log.warn("File is not ready");
                        }
                        return FileMapper.toDownloadFile(item, result);
                    })
                    .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
        }

        String uid= UUID.randomUUID().toString();
        PnDeliveryFile file = new PnDeliveryFile();
        file.setUuid(uid);
        file.setStatus(InfoDownloadDTO.StatusEnum.UPLOADING.getValue());

        return fileDownloadDAO.create(file)
                .map(item -> {
                    createAndUploadFileAsync(tenderCode, item.getUuid());
                    return FileMapper.toDownloadFile(item, null);
                });
    }

    @Override
    public Mono<NotifyResponseDto> notifyUpload(String tenderCode, NotifyUploadRequestDto uploadRequestDto) {
        if (StringUtils.isEmpty(uploadRequestDto.getUuid())) {
            return Mono.error(new PnGenericException(ExceptionTypeEnum.BADLY_REQUEST, ExceptionTypeEnum.BADLY_REQUEST.getMessage(), HttpStatus.BAD_REQUEST));
        }

        return fileDownloadDAO.getUuid(uploadRequestDto.getUuid())
                .switchIfEmpty(Mono.error(new PnGenericException(FILE_REQUEST_ASYNC_NOT_FOUND, FILE_REQUEST_ASYNC_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)))
                .flatMap(item -> {
                    if (StringUtils.equals(item.getStatus(), FileStatusCodeEnum.ERROR.getCode())) {
                        if (CollectionUtils.isEmpty(item.getErrorMessage().getErrorDetails())) {
                            return Mono.error(new PnGenericException(ExceptionTypeEnum.EXCEL_BADLY_CONTENT, item.getErrorMessage().getMessage()));
                        }
                        return Mono.error(new PnExcelValidatorException(ExceptionTypeEnum.BADLY_REQUEST, ErrorDetailMapper.toDtos(item.getErrorMessage().getErrorDetails())));

                    } else if (StringUtils.equals(item.getStatus(), FileStatusCodeEnum.UPLOADING.getCode())) {
                        String fileName = S3Bucket.PREFIX_URL + uploadRequestDto.getUuid();
                        InputStream inputStream = s3Bucket.getFileInputStream(fileName);
                        if (inputStream != null) {
                            notifyUploadAsync(item, inputStream, tenderCode).subscribeOn(Schedulers.parallel()).subscribe();
                            item.setStatus(FileStatusCodeEnum.IN_PROGRESS.getCode());
                            return this.fileDownloadDAO.create(item).map(NotifyResponseMapper::toDto);
                        }
                    }
                    return Mono.just(NotifyResponseMapper.toDto(item));
                });
    }

    public Mono<Void> notifyUploadAsync(PnDeliveryFile item, InputStream inputStream, String tenderCode){
        return Mono.just("")
                .map(nothing -> this.excelDAO.readData(inputStream))
                .map(deliveriesData -> DeliveryDriverMapper.toEntityFromExcel(deliveriesData, tenderCode))
                .zipWhen(driversAndCost -> deleteAllDriverFromTender(tenderCode), (a, b) -> a )
                .flatMap(this::createFromMap)
                .flatMap(nothing -> {
                    item.setStatus(FileStatusCodeEnum.COMPLETE.getCode());
                    return fileDownloadDAO.create(item).then();
                })
                .onErrorResume(ex -> {
                    item.setStatus(FileStatusCodeEnum.ERROR.getCode());
                    if (ex instanceof PnExcelValidatorException){
                        item.setErrorMessage(ErrorMessageMapper.toEntity((PnExcelValidatorException)ex));
                    } else {
                        PnErrorMessage pnErrorMessage = new PnErrorMessage();
                        pnErrorMessage.setMessage(ex.getMessage());
                        item.setErrorMessage(pnErrorMessage);
                    }

                    return  fileDownloadDAO.create(item).flatMap(entity -> Mono.error(ex));
                });

    }

    private Mono<String> createFromMap(Map<PnDeliveryDriver, List<PnCost>> driversAndCosts) {
        return  Flux.fromStream(driversAndCosts.keySet().stream())
                .flatMap(driver -> deliveryDriverDAO.createOrUpdate(driver)
                                        .flatMap(driverEntity -> Flux.fromStream(driversAndCosts.get(driver).stream())
                                                                    .flatMap(cost -> this.costDAO.createOrUpdate(cost))
                                                                    .collectList()
                                        ).map(costs -> driver)
                ).collectList()
                .map(list -> "");

    }

    private Mono<String> deleteAllDriverFromTender(String tenderCode) {
        return this.deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, null)
                .flatMap(driver -> this.deleteDriver(tenderCode, driver.getTaxId()))
                .collectList()
                .map(list -> tenderCode);
    }

    private void createAndUploadFileAsync(String tenderCode,String uuid){
        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  {
                    if (StringUtils.isNotBlank(tenderCode)) {
                        return this.deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, null)
                                .collectList()
                                .zipWhen(drivers -> this.costDAO.findAllFromTenderCode(tenderCode, null).collectList())
                                .flatMap(driversAndCosts -> {
                                    ExcelEngine excelEngine = this.excelDAO.create(ExcelModelMapper.fromDeliveriesDrivers(driversAndCosts.getT1(),driversAndCosts.getT2()));
                                    File file = excelEngine.saveOnDisk();
                                    return Mono.just(file);
                                });
                    } else {
                        ExcelEngine excelEngine = this.excelDAO.create(new DeliveriesData());
                        File f = excelEngine.saveOnDisk();
                        return Mono.just(f);
                    }
                })
                .publishOn(Schedulers.boundedElastic())
                .zipWhen(s3Bucket::putObject)
                .zipWhen(file -> fileDownloadDAO.getUuid(uuid))
                .map(entityAndFile -> Tuples.of(entityAndFile.getT1().getT1(), entityAndFile.getT2()))
                .flatMap(entityAndFile -> {
                    entityAndFile.getT2().setFilename(entityAndFile.getT1().getName());
                    entityAndFile.getT2().setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());
                    // save item and delete file
                    return fileDownloadDAO.create(entityAndFile.getT2())
                            .map(file -> {
                                boolean delete = entityAndFile.getT1().delete();
                                log.debug("Deleted file "+delete);
                                return FileMapper.toDownloadFile(entityAndFile.getT2(), s3Bucket.getObjectData(entityAndFile.getT2().getFilename()));
                            });
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public Mono<TenderCreateResponseDTO> createOrUpdateTender(TenderCreateRequestDTO request) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        final boolean isCreated = (request.getCode() == null);
        if (request.getEndDate().before(request.getStartDate())){
            return Mono.error(new PnGenericException(ExceptionTypeEnum.BADLY_DATE_INTERVAL, ExceptionTypeEnum.BADLY_DATE_INTERVAL.getMessage()));
        }

        if (isCreated) pnLogAudit.addsBeforeCreate("Create Tender");
        else pnLogAudit.addsBeforeUpdate("Update Tender");

        //set 00:00
        request.getStartDate().setTime(DateUtils.formatDateWithSpecificHour(request.getStartDate(), 0,0,0).getTime());
        //set 23:59
        request.getEndDate().setTime(DateUtils.formatDateWithSpecificHour(request.getEndDate(), 23,59,0).getTime());
        return Mono.just(TenderMapper.toTenderRequest(request))
                .flatMap(entity -> this.tenderDAO.createOrUpdate(entity))
                .map(entity -> {
                    if (isCreated) pnLogAudit.addsSuccessCreate("Create Tender OK:"+ Utility.objectToJson(entity));
                    TenderCreateResponseDTO response = new TenderCreateResponseDTO();
                    response.setTender(TenderMapper.tenderToDto(entity));
                    response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    if(!isCreated) pnLogAudit.addsSuccessUpdate("Update Tender OK:"+ Utility.objectToJson(entity));
                    return response;
                })
                .onErrorResume(ex -> {
                    if (isCreated) pnLogAudit.addsFailCreate("Create Tender ERROR");
                    else pnLogAudit.addsFailUpdate("Update Tender ERROR");
                    return Mono.error(ex);
                });
    }

    @Override
    public Mono<Void> createOrUpdateDriver(String tenderCode, DeliveryDriverDTO request) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        AtomicBoolean isCreated = new AtomicBoolean(false);
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(ExceptionTypeEnum.TENDER_NOT_EXISTED, ExceptionTypeEnum.TENDER_NOT_EXISTED.getMessage())))
                .flatMap(tender -> this.deliveryDriverDAO.getDeliveryDriver(tenderCode, request.getTaxId())
                            .switchIfEmpty(Mono.just(new PnDeliveryDriver()))
                            .flatMap(driver -> {
                                if (driver == null || StringUtils.isBlank(driver.getTaxId())) {
                                    isCreated.set(true);
                                    pnLogAudit.addsBeforeCreate("Create DeliveryDriver");
                                    return Mono.just(tender);
                                }
                                if (Boolean.compare(driver.fsu, request.getFsu())!=0) {
                                    return Mono.error(new PnGenericException(DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE, DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE.getMessage()));
                                }
                                pnLogAudit.addsBeforeUpdate("Update DeliveryDriver");
                                return Mono.just(tender);
                            })
                )
                .map(tender -> {
                    log.info("Gara recuperata");
                    PnDeliveryDriver driver = DeliveryDriverMapper.toEntity(request);
                    driver.setTenderCode(tenderCode);
                    return driver;
                })
                .flatMap(entity -> this.deliveryDriverDAO.createOrUpdate(entity))
                .onErrorResume(ex -> {
                    if (isCreated.get()) pnLogAudit.addsFailCreate("Create DeliveryDriver ERROR");
                    if (!isCreated.get()) pnLogAudit.addsFailUpdate("Update DeliveryDriver ERROR");
                    return Mono.error(ex);
                })
                .mapNotNull(entity -> {
                    if (isCreated.get()) pnLogAudit.addsSuccessCreate("Create DeliveryDriver OK:"+ Utility.objectToJson(entity));
                    if (!isCreated.get()) pnLogAudit.addsSuccessUpdate("Update DeliveryDriver OK:"+ Utility.objectToJson(entity));
                    return null;
                });
    }



    @Override
    public Mono<Void> createOrUpdateCost(String tenderCode, String taxId, CostDTO request) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        AtomicBoolean isCreated = new AtomicBoolean(false);
        AtomicBoolean isUpdated = new AtomicBoolean(false);
        if ((request.getCap() == null || request.getCap().isEmpty()) && request.getZone() == null){
            return Mono.error(new PnGenericException(COST_BADLY_CONTENT, COST_BADLY_CONTENT.getMessage()));
        }
        if (request.getUid() == null) {
            isCreated.set(true);
            pnLogAudit.addsBeforeCreate("Create Cost");
        }
        else{
            isUpdated.set(true);
            pnLogAudit.addsBeforeCreate("Update Cost");
        }
        return this.deliveryDriverDAO.getDeliveryDriver(tenderCode, taxId)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage())))
                .flatMap(driver -> {
                    PnCost fromRequest = CostMapper.fromCostDTO(driver.getTenderCode(), driver.getTaxId(), request);
                    fromRequest.setFsu(driver.getFsu());
                    String code = request.getUid();
                    return this.costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(tenderCode, fromRequest.getProductType(), code)
                            .collectList()
                            .zipWhen(listFromDB -> Mono.just(fromRequest));
                })
                .flatMap(fromDbAndFromRequest -> {
                    List<PnCost> fromDB = fromDbAndFromRequest.getT1();
                    PnCost fromRequest = fromDbAndFromRequest.getT2();
                    if (fromRequest.getCap() != null && fromRequest.getZone() == null){
                        List<String> caps = new ArrayList<>();
                        fromDB.forEach(cost -> {
                            if (cost.getCap() != null){
                                caps.addAll(cost.getCap());
                            }
                        });
                        CostValidator.validateCosts(caps, fromRequest.getCap());
                    }
                    return this.costDAO.createOrUpdate(fromRequest)
                            .onErrorResume(ex -> {
                                if (!isCreated.get()) pnLogAudit.addsFailCreate("Create Cost ERROR");
                                if (!isUpdated.get()) pnLogAudit.addsFailUpdate("Update Cost ERROR");
                                return Mono.error(ex);
                            })
                            .flatMap(item -> {
                                if (isCreated.get()) pnLogAudit.addsSuccessCreate("Create Cost OK:"+ Utility.objectToJson(item));
                                if (isUpdated.get()) pnLogAudit.addsSuccessUpdate("Update Cost OK:"+ Utility.objectToJson(item));
                                return Mono.empty();
                            });
                });
    }

    @Override
    public Mono<Void> deleteTender(String tenderCode) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        pnLogAudit.addsBeforeDelete("Delete Tender with tenderCode:" + tenderCode);
        return this.tenderWithCreatedStatus(tenderCode, TENDER_CANNOT_BE_DELETED)
                .flatMap(tender -> this.deliveryDriverDAO.getDeliveryDriverFromTender(tender.getTenderCode(),null)
                                        .delayElements(Duration.ofMillis(10))
                                        .flatMap(driver -> this.deleteDriver(driver.getTenderCode(), driver.getTaxId()))
                                        .collectList()
                )
                .flatMap(items -> this.tenderDAO.deleteTender(tenderCode))
                .onErrorResume(ex ->{
                    pnLogAudit.addsFailDelete("Delete Tender with tenderCode:" +tenderCode+ "ERROR");
                    return Mono.error(ex);
                })
                .flatMap(item -> {
                    pnLogAudit.addsSuccessDelete("Delete Tender with tenderCode"+tenderCode+ "OK");
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> deleteDriver(String tenderCode, String deliveryDriverId) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        pnLogAudit.addsBeforeDelete("Delete DeliveryDriver with tenderCode:" + tenderCode);
        return this.tenderWithCreatedStatus(tenderCode, DRIVER_CANNOT_BE_DELETED)
                        .flatMap(tender -> this.costDAO.findAllFromTenderCode(tenderCode,deliveryDriverId)
                                        .delayElements(Duration.ofMillis(10))
                                        .flatMap(cost -> this.costDAO.deleteCost(cost.getDeliveryDriverCode(),cost.getUuid()))
                                        .collectList()
                        )
                        .flatMap(costs -> this.deliveryDriverDAO.deleteDeliveryDriver(tenderCode,deliveryDriverId))
                        .onErrorResume(ex -> {
                            pnLogAudit.addsFailDelete("Delete DeliveryDriver with tenderCode:" +tenderCode+ "ERROR");
                            return Mono.error(ex);
                        })
                        .flatMap(driver -> {
                            pnLogAudit.addsSuccessDelete("Delete DeliveryDriver with tenderCode:" +tenderCode+ "OK");
                            return Mono.empty();
                        });

    }

    @Override
    public Mono<Void> deleteCost(String tenderCode, String deliveryDriverId, String uuid) {
        PnLogAudit pnLogAudit = new PnLogAudit(pnAuditLogBuilder);
        pnLogAudit.addsBeforeDelete("Delete Cost with tenderCode:" + tenderCode);
        return this.tenderWithCreatedStatus(tenderCode, COST_CANNOT_BE_DELETED)
                .flatMap(tender -> this.costDAO.deleteCost(deliveryDriverId,uuid))
                .onErrorResume(ex ->{
                    pnLogAudit.addsFailDelete("Delete Cost with tenderCode:" +tenderCode+ "ERROR");
                    return Mono.error(ex);
                })
                .flatMap(cost -> {
                    pnLogAudit.addsSuccessDelete("Delete Cost with tenderCode:" +tenderCode+ "OK");
                    return Mono.empty();
                });
    }


    private Mono<PnTender> tenderWithCreatedStatus(String tenderCode, ExceptionTypeEnum typeException){
        return this.tenderDAO.getTender(tenderCode)
                .flatMap(tender -> {
                    if (!tender.status.equals(TenderDTO.StatusEnum.CREATED.toString())){
                        return Mono.error(new PnGenericException(typeException, typeException.getMessage()));
                    }
                    return Mono.just(tender);
                });
    }


    @Override
    public Mono<TenderCreateResponseDTO> updateStatusTender(String tenderCode, Status status) {
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(TENDER_NOT_EXISTED, TENDER_NOT_EXISTED.getMessage())))
                .flatMap(entity -> {
                    TenderCreateResponseDTO response = new TenderCreateResponseDTO();
                    if (entity.getActualStatus().equals(TenderDTO.StatusEnum.IN_PROGRESS.getValue()) ||
                            entity.getActualStatus().equals(TenderDTO.StatusEnum.ENDED.getValue())) {
                        return Mono.error(new PnGenericException(STATUS_NOT_VARIABLE, STATUS_NOT_VARIABLE.getMessage()));
                    }
                    if (!entity.getStatus().equalsIgnoreCase(status.getStatusCode().getValue()) &&
                            entity.getStatus().equalsIgnoreCase(TenderDTO.StatusEnum.CREATED.getValue())) {
                        return this.tenderDAO.getConsolidate(entity.getStartDate(), entity.getEndDate())
                                .flatMap(newTender ->
                                        Mono.error(new PnGenericException(CONSOLIDATE_ERROR, CONSOLIDATE_ERROR.getMessage()))
                                )
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            return this.isValidFSUCost(tenderCode)
                                                            .map(isValid -> {
                                                                if (Boolean.FALSE.equals(isValid)){
                                                                    throw new PnGenericException(FSUCOST_VALIDATOR_NOTVALID, FSUCOST_VALIDATOR_NOTVALID.getMessage());
                                                                }
                                                                entity.setStatus(status.getStatusCode().getValue());
                                                                return this.tenderDAO.createOrUpdate(entity)
                                                                        .map(modifyEntity -> {
                                                                            response.setTender(TenderMapper.tenderToDto(modifyEntity));
                                                                            response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                                                                            response.setResult(true);
                                                                            return response;
                                                                        });
                                                            });

                                        })
                                );
                    } else if (!entity.getStatus().equalsIgnoreCase(status.getStatusCode().getValue()) &&
                            entity.getStatus().equalsIgnoreCase(TenderDTO.StatusEnum.VALIDATED.getValue())) {
                        entity.setStatus(status.getStatusCode().getValue());
                        return this.tenderDAO.createOrUpdate(entity)
                                .map(modifyEntity -> {
                                    response.setTender(TenderMapper.tenderToDto(modifyEntity));
                                    response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                                    response.setResult(true);
                                    return response;
                                });
                    }
                    response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    return Mono.just(response);
                }).mapNotNull(entity -> null);
    }


    private Mono<Boolean> isValidFSUCost(String tenderCode){
        return this.deliveryDriverDAO.getDeliveryDriverFSU(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(COST_DRIVER_OR_FSU_NOT_FOUND, COST_DRIVER_OR_FSU_NOT_FOUND.getMessage())))
                .flatMap(fsu -> this.costDAO.findAllFromTenderCode(tenderCode, fsu.getTaxId()).collectList())
                .map(costs-> {
                    if (costs.isEmpty()) {
                        throw new PnGenericException(COST_DRIVER_OR_FSU_NOT_FOUND, COST_DRIVER_OR_FSU_NOT_FOUND.getMessage());
                    }
                    Map<String, Boolean> mapValidationCost = Utility.requiredCostFSU();
                    costs.forEach(cost -> {
                        String key = "";
                        if (StringUtils.isNotBlank(cost.getZone())){
                            key = cost.getZone()+"-"+cost.getProductType();
                            mapValidationCost.put(key, true);

                        } else if (cost.getCap() != null && !cost.getCap().isEmpty() && cost.getCap().contains(Const.CAP_DEFAULT)){
                            key = Const.CAP_DEFAULT+"-"+cost.getProductType();
                            mapValidationCost.put(key, true);
                        }
                    });
                    return mapValidationCost;
                })
                .map(mapValidation ->  mapValidation.values().stream().filter(Boolean.FALSE::equals).toList().isEmpty());
    }

}
