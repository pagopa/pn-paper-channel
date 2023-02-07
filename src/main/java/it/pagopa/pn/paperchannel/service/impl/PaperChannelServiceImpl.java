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
    public Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page-1, size);
        return deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode)
                .map(list ->
                        DeliveryDriverMapper.toPagination(pageable, list)
                )
                .map(DeliveryDriverMapper::toPageableResponse);
    }

    @Override
    public Mono<AllPricesContractorResponseDto> getAllPricesOfDeliveryDriver(String tenderCode, String deliveryDriver) {
        return costDAO.retrievePrice(tenderCode, deliveryDriver)
                .map(CostMapper::toResponse);
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
                    .map(item -> FileMapper.toDownloadFile(item, s3Bucket.getObjectData(item.getFilename())))
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
        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  {
                    if (StringUtils.isNotBlank(tenderCode)) {
                        return this.deliveryDriverDAO.getDeliveryDriverFromTender(tenderCode)
                                .zipWhen(drivers -> this.costDAO.retrievePrice(tenderCode,null))
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
    public Mono<Void> createOrUpdateDriver(String tenderCode, DeliveryDriverDto request) {
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(ExceptionTypeEnum.TENDER_NOT_EXISTED, ExceptionTypeEnum.TENDER_NOT_EXISTED.getMessage())))
                .map(tender -> {
                    PnDeliveryDriver driver = DeliveryDriverMapper.toEntity(request);
                    driver.setTenderCode(tenderCode);
                    return driver;
                }).flatMap(entity -> this.deliveryDriverDAO.createOrUpdate(entity))
                .mapNotNull(entity -> null);
    }

    @Override
    public Mono<Void> createOrUpdateCost(String deliveryDriverCode, CostDTO request) {
        if (StringUtils.isBlank(request.getCap()) && request.getZone() == null){
            throw new PnGenericException(COST_BADLY_CONTENT, COST_BADLY_CONTENT.getMessage());
        }
        return this.deliveryDriverDAO.getDeliveryDriverFromCode(deliveryDriverCode)
                .flatMap(driver -> {
                    String tenderCode = driver.getTenderCode();
                    List<PnCost> fromRequest = CostMapper.toEntity(tenderCode, driver.uniqueCode, request);
                    return this.costDAO.retrievePrice(tenderCode, null, (request.getZone() == null))
                            .zipWhen(listFromDB -> Mono.just(fromRequest));
                })
                .flatMap(fromDbAndFromRequest -> {
                    List<PnCost> fromDB = fromDbAndFromRequest.getT1();
                    List<PnCost> fromRequest = fromDbAndFromRequest.getT2();
                    //Call validator
                    return this.costDAO.createOrUpdate(fromRequest).flatMap(item -> Mono.empty());
                });

        //retrieve deliveryDriver with deliveryDriverCode - DONE
        //get tenderCode from deliveryDriver - DONE
        // retrieve all cost with tenderCode - DONE
            // check if request is a valid cost -DONE
                // - if National -> CAP, PRODUCT-TYPE not could exist occurrences
                // - if International -> ZONE, PRODUCT-TYPE not could exist occurrences - product-type : AR o SEMPLICE
            // if validation doesn't return any error, you can update the cost
    }


}
