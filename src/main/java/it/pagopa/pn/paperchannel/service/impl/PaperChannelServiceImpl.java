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

import java.io.File;
import java.time.Duration;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;


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
                .map(presignedUrlResponse -> presignedUrlResponse);
    }

    public Mono<InfoDownloadDTO> downloadTenderFile(String tenderCode,String uuid) {
        if(StringUtils.isNotEmpty(uuid)) {
            return fileDownloadDAO.getUuid(uuid)
                    .map(FileMapper::toDownloadFile)
                    .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(), HttpStatus.NOT_FOUND)));
        }

        String uid= UUID.randomUUID().toString();
        PnDeliveryFile file = new PnDeliveryFile();
        file.setUuid(uid);
        file.setStatus(InfoDownloadDTO.StatusEnum.UPLOADING.getValue());

        return fileDownloadDAO.create(file)
                .map(item -> {
                    createAndUploadFileAsync(tenderCode, item.getUuid());
                    return FileMapper.toDownloadFile(item);
                });
    }

    private void createAndUploadFileAsync(String tenderCode,String uuid){
        DeliveriesData excelModel = new DeliveriesData();

        if (StringUtils.isNotBlank(tenderCode)) {
            this.deliveryDriverDAO.getDeliveryDriver(tenderCode)
                    .zipWhen(drivers -> this.costDAO.retrievePrice(tenderCode,null))
                    .map(driversAndCosts -> {

                        ExcelEngine excelEngine = this.excelDAO.create(ExcelModelMapper.fromDeliveriesDrivers(driversAndCosts.getT1(),driversAndCosts.getT2()));
                        File fIle = excelEngine.saveOnDisk();

                        // save file on s3 bucket and update entity
                        Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                                .flatMap(i ->  s3Bucket.putObject(fIle)
                                        .zipWhen(url -> fileDownloadDAO.getUuid(uuid)))
                                .publishOn(Schedulers.boundedElastic())
                                .map(entity -> {
                                    fIle.delete();
                                    entity.getT2().setUrl(entity.getT1());
                                    entity.getT2().setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());
                                    fileDownloadDAO.create(entity.getT2());
                                    return Mono.empty();
                                }).subscribeOn(Schedulers.boundedElastic()).subscribe();

                        return Mono.just("");
                    });
        } else {
            ExcelEngine excelEngine = this.excelDAO.create(excelModel);
            File f = excelEngine.saveOnDisk();

            // save file on s3 bucket and update entity
            Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())
                    .flatMap(i ->  s3Bucket.putObject(f)
                    .zipWhen(url -> fileDownloadDAO.getUuid(uuid)))
                    .publishOn(Schedulers.boundedElastic())
                    .map(entity -> {
                        f.delete();
                        entity.getT2().setUrl(entity.getT1());
                        entity.getT2().setStatus(InfoDownloadDTO.StatusEnum.UPLOADED.getValue());
                        fileDownloadDAO.create(entity.getT2());
                        return Mono.empty();
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }
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
