package it.pagopa.pn.paperchannel.service.impl;

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
import it.pagopa.pn.paperchannel.validator.CostValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;

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
                            log.warn("File is no ready");
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
    public Mono<NotifyResponseDto> notifyUpload(TenderUploadRequestDto uploadRequestDto) {
        if (StringUtils.isEmpty(uploadRequestDto.getUuid())) {
            return Mono.error(new PnGenericException(ExceptionTypeEnum.BADLY_REQUEST, ExceptionTypeEnum.BADLY_REQUEST.getMessage(), HttpStatus.BAD_REQUEST));
        }

        return fileDownloadDAO.getUuid(uploadRequestDto.getUuid())
                .map(item -> {
                    if (StringUtils.equals(item.getStatus(), FileStatusCodeEnum.ERROR.getCode())) {
                        if (CollectionUtils.isEmpty(item.getErrorMessage().getErrorDetails())) {
                            throw new PnGenericException(ExceptionTypeEnum.EXCEL_BADLY_CONTENT, item.getErrorMessage().getMessage());
                        }
                        throw new PnExcelValidatorException(ExceptionTypeEnum.BADLY_REQUEST, ErrorDetailMapper.toDtos(item.getErrorMessage().getErrorDetails()));
                    } else if (!StringUtils.equals(item.getStatus(), FileStatusCodeEnum.UPLOADING.getCode())) {
                        return NotifyResponseMapper.toDto(item);
                    } else {
                        String fileName = S3Bucket.PREFIX_URL + uploadRequestDto.getUuid();
                        InputStream inputStream = s3Bucket.getFileInputStream(fileName);
                        if (inputStream != null) {
                            notifyUploadAsync(item, inputStream, uploadRequestDto);
                            return NotifyResponseMapper.toDto(item);
                        }
                    }
                    return null;
                }).switchIfEmpty(Mono.error(new PnGenericException(ExceptionTypeEnum.FILE_REQUEST_ASYNC_NOT_FOUND, ExceptionTypeEnum.FILE_REQUEST_ASYNC_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)));
    }

    public void notifyUploadAsync(PnDeliveryFile item, InputStream inputStream, TenderUploadRequestDto tenderRequest){
        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.boundedElastic())
                .map(i -> this.excelDAO.readData(inputStream))
                .zipWhen(deliveriesData -> {
                    PnTender tender = TenderMapper.toTender(tenderRequest);
                    Map<PnDeliveryDriver, List<PnCost>> map = DeliveryDriverMapper.toEntityFromExcel(deliveriesData, tender.getTenderCode());
                    return this.tenderDAO.createNewContract(map,tender);
                })
                .map(i -> {
                    item.setStatus(FileStatusCodeEnum.COMPLETE.getCode());
                    return fileDownloadDAO.create(item);
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
                    fileDownloadDAO.create(item);
                    return Mono.error(ex);
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private void createAndUploadFileAsync(String tenderCode,String uuid){
        Mono.delay(Duration.ofMillis(25000)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  {
                    if (StringUtils.isNotBlank(tenderCode)) {
                        return this.deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode, false)
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
        if (request.getEndDate().before(request.getStartDate())){
            throw new PnGenericException(ExceptionTypeEnum.BADLY_DATE_INTERVAL, ExceptionTypeEnum.BADLY_DATE_INTERVAL.getMessage());
        }
        return Mono.just(TenderMapper.toTenderRequest(request))
                .flatMap(entity -> this.tenderDAO.createOrUpdate(entity))
                .map(entity -> {
                    TenderCreateResponseDTO response = new TenderCreateResponseDTO();
                    response.setTender(TenderMapper.tenderToDto(entity));
                    response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                    response.setResult(true);
                    return response;
                });
    }

    @Override
    public Mono<Void> createOrUpdateDriver(String tenderCode, DeliveryDriverDTO request) {
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(ExceptionTypeEnum.TENDER_NOT_EXISTED, ExceptionTypeEnum.TENDER_NOT_EXISTED.getMessage())))
                .flatMap(tender -> this.deliveryDriverDAO.getDeliveryDriver(tenderCode, request.getTaxId())
                            .switchIfEmpty(Mono.just(new PnDeliveryDriver()))
                            .flatMap(driver -> {
                                if (driver == null || StringUtils.isBlank(driver.getTaxId())) return Mono.just(tender);
                                if (Boolean.compare(driver.fsu, request.getFsu())!=0) {
                                    return Mono.error(new PnGenericException(DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE, DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE.getMessage()));
                                }
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
                .mapNotNull(entity -> null);
    }

    @Override
    public Mono<Void> createOrUpdateCost(String tenderCode, String taxId, CostDTO request) {
        if ((request.getCap() == null || request.getCap().isEmpty()) && request.getZone() == null){
            return Mono.error(new PnGenericException(COST_BADLY_CONTENT, COST_BADLY_CONTENT.getMessage()));
        }

        return this.deliveryDriverDAO.getDeliveryDriver(tenderCode, taxId)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage())))
                .flatMap(driver -> {
                    PnCost fromRequest = CostMapper.fromCostDTO(driver.getTenderCode(), driver.getTaxId(), request);
                    String code = request.getUid();
                    return this.costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(tenderCode, fromRequest.getProductType(), code)
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
                    return this.costDAO.createOrUpdate(fromRequest).flatMap(item -> Mono.empty());
                });
    }


}
