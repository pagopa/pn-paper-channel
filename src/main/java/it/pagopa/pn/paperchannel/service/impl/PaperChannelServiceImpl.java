package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.FILE_NOT_FOUND;


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
    public Mono<PageableDeliveryDriverResponseDto> getAllDeliveriesDrivers(String tenderCode, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page-1, size);
        return deliveryDriverDAO.getDeliveryDriver(tenderCode)
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
    public Mono<BaseResponse> notifyUpload(TenderUploadRequestDto uploadRequestDto) {
        String nameFile = S3Bucket.PREFIX_URL + uploadRequestDto.getUuid();
        return Mono.just(1)
                .map(i -> s3Bucket.getFileInputStream(nameFile))
                .map(inputStream -> this.excelDAO.readData(inputStream))
                .flatMap(deliveriesData -> {
                    String tenderCode = UUID.randomUUID().toString();
                    PnTender tender = new PnTender();
                    tender.setTenderCode(tenderCode);
                    tender.setStatus("CREATED");
                    tender.setDate(Instant.now());
                    tender.setEndDate(uploadRequestDto.getTender().getEndDate().toInstant());
                    tender.setStartDate(uploadRequestDto.getTender().getStartDate().toInstant());
                    tender.setAuthor("PN-PAPER-CHANNEL");
                    tender.setDescription(uploadRequestDto.getTender().getName());
                    Map<PnDeliveryDriver, List<PnCost>> map = DeliveryDriverMapper.toEntityFromExcel(deliveriesData, tenderCode);
                    return this.costDAO.createNewContract(map,tender);
                })
                .map(tender -> new BaseResponse())
                .switchIfEmpty(Mono.error(new PnGenericException(FILE_NOT_FOUND, FILE_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)));
    }

    private void createAndUploadFileAsync(String tenderCode,String uuid){
        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                .flatMap(i ->  {
                    if (StringUtils.isNotBlank(tenderCode)) {
                        return this.deliveryDriverDAO.getDeliveryDriver(tenderCode)
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
                .zipWhen(entityAndFile -> {
                    entityAndFile.getT2().setFilename(entityAndFile.getT1().getName());
                    entityAndFile.getT2().setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());
                    // save item and delete file
                    return fileDownloadDAO.create(entityAndFile.getT2())
                            .map(file -> {
                                entityAndFile.getT1().delete();
                                return FileMapper.toDownloadFile(entityAndFile.getT2(), s3Bucket.getObjectData(entityAndFile.getT2().getFilename()));
                            });
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    /*
    @Override
    public Mono<BaseResponse> createContract(ContractInsertRequestDto request) {
        PnPaperDeliveryDriver pnPaperDeliveryDriver = DeliveryDriverMapper.toContractRequest(request);
        List<PnPaperCost> costs = request.getList().stream().map(CostMapper::fromContractDTO).collect(Collectors.toList());
        return this.costDAO.createNewContract(pnPaperDeliveryDriver, costs).map(deliveryDriver -> {
            BaseResponse baseResponse = new BaseResponse();
            baseResponse.setResult(true);
            baseResponse.setCode(BaseResponse.CodeEnum.NUMBER_0);
            return baseResponse;
        });
    }

    @Override
    public Mono<PageableDeliveryDriverResponseDto> takeDeliveryDriver(DeliveryDriverFilter filter) {
        Pageable pageable = PageRequest.of(filter.getPage()-1, filter.getSize());
        return deliveryDriverDAO.getDeliveryDriver(filter)
                .map(list -> DeliveryDriverMapper.paginateList(pageable, list))
                .map(DeliveryDriverMapper::deliveryDriverToPageableDeliveryDriverDto);
    }
*/

}
