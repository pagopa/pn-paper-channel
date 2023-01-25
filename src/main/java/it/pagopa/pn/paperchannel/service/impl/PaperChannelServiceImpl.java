package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.dao.ExcelDAO;
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
                .map(presignedUrl -> PresignedUrlResponseMapper.fromResult(presignedUrl));
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

        return fileDownloadDAO.create(file).map(FileMapper::toDownloadFile);
        // .map(chiamare flusso asyncrono per generare e caricare il file  "createAndUploadFileAsync()");

    }

    private void createAndUploadFileAsync(String tenderCode,String uuid){
        DeliveriesData excelModel = new DeliveriesData();
        if(StringUtils.isNotBlank(tenderCode)){
            this.deliveryDriverDAO.getDeliveryDriver(tenderCode)
                    .zipWhen(drivers -> this.costDAO.retrievePrice(tenderCode,null))
                    .map(driversAndCosts -> {
                        this.excelDAO.createAndSave(ExcelModelMapper.fromDeliveriesDrivers(driversAndCosts.getT1(),driversAndCosts.getT2()));
                        //prendere il file e salvarlo su S3
                        //aggiornare il DB (settare nuovamente il pnFile con i nuovi parametri)

                        return Mono.just("");
                    });
        } else {
            this.excelDAO.createAndSave(excelModel);
            //prendere il file e salvarlo su S3
            //aggiornare il DB (settare nuovamente il pnFile con i nuovi parametri)
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
