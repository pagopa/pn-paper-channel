package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.mapper.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import it.pagopa.pn.paperchannel.validator.CostValidator;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;


@CustomLog
@Service
@RequiredArgsConstructor
public class PaperChannelServiceImpl implements PaperChannelService {
    private final CostDAO costDAO;
    private final DeliveryDriverDAO deliveryDriverDAO;
    private final TenderDAO tenderDAO;
    private final FileDownloadDAO fileDownloadDAO;


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
    public Mono<TenderCreateResponseDTO> createOrUpdateTender(TenderCreateRequestDTO request) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        final boolean isCreated = (request.getCode() == null);
        if (request.getEndDate().before(request.getStartDate())){
            return Mono.error(new PnGenericException(ExceptionTypeEnum.BADLY_DATE_INTERVAL, ExceptionTypeEnum.BADLY_DATE_INTERVAL.getMessage()));
        }

        if (isCreated) pnLogAudit.addsBeforeCreate("Create Tender");
        else pnLogAudit.addsBeforeUpdate("Update Tender");


        return Mono.just(TenderMapper.toTenderRequest(request))
                .flatMap(this.tenderDAO::createOrUpdate)
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
        PnLogAudit pnLogAudit = new PnLogAudit();
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
        PnLogAudit pnLogAudit = new PnLogAudit();
        final boolean isCreated = (request.getUid() == null);

        if ((request.getCap() == null || request.getCap().isEmpty()) && request.getZone() == null){
            return Mono.error(new PnGenericException(COST_BADLY_CONTENT, COST_BADLY_CONTENT.getMessage()));
        }

        if (isCreated) pnLogAudit.addsBeforeCreate("Create Cost");
        else pnLogAudit.addsBeforeCreate("Update Cost");

        return this.deliveryDriverDAO.getDeliveryDriver(tenderCode, taxId)
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_DRIVER_NOT_EXISTED, DELIVERY_DRIVER_NOT_EXISTED.getMessage())))
                .map(driver -> {
                    PnCost fromRequest = CostMapper.fromCostDTO(driver.getTenderCode(), driver.getTaxId(), request);
                    fromRequest.setFsu(driver.getFsu());
                    return fromRequest;
                })
                .zipWhen(pnCost -> this.costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(tenderCode, pnCost.getProductType(), pnCost.getUuid())
                            .collectList()
                )
                .map(costAndListCost -> {
                    PnCost pnCost = costAndListCost.getT1();
                    List<PnCost> fromDB = costAndListCost.getT2();

                    if (pnCost.getCap() != null && pnCost.getZone() == null) {
                        List<String> caps = new ArrayList<>();
                        fromDB.forEach(cost -> {
                            if (cost.getCap() != null) {
                                caps.addAll(cost.getCap());
                            }
                        });
                        CostValidator.validateCosts(caps, pnCost.getCap());
                    }
                    return pnCost;
                })
                .flatMap(pnCost -> this.costDAO.createOrUpdate(pnCost))
                .onErrorResume(ex -> {
                    if (isCreated) pnLogAudit.addsFailCreate("Create Cost ERROR");
                    else pnLogAudit.addsFailUpdate("Update Cost ERROR");
                    return Mono.error(ex);
                })
                .flatMap(item -> {
                    if (isCreated) pnLogAudit.addsSuccessCreate("Create Cost OK:"+ Utility.objectToJson(item));
                    else pnLogAudit.addsSuccessUpdate("Update Cost OK:"+ Utility.objectToJson(item));
                    return Mono.empty();
                });
    }


    @Override
    public Mono<Void> deleteTender(String tenderCode) {
        PnLogAudit pnLogAudit = new PnLogAudit();
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
        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeDelete("Delete DeliveryDriver with tenderCode:" + tenderCode);
        return this.tenderWithCreatedStatus(tenderCode, DRIVER_CANNOT_BE_DELETED)
                        .flatMap(tender -> this.costDAO.findAllFromTenderCode(tenderCode,deliveryDriverId)
                                        .delayElements(Duration.ofMillis(10))
                                        .flatMap(cost -> this.costDAO.deleteCost(cost.getDeliveryDriverCode(),cost.getUuid()))
                                        .collectList()
                        )
                        .flatMap(costs -> this.deliveryDriverDAO.deleteDeliveryDriver(tenderCode, deliveryDriverId))
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
        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeDelete("Delete Cost with tenderCode:" + tenderCode);
        return this.tenderWithCreatedStatus(tenderCode, COST_CANNOT_BE_DELETED)
                .flatMap(tender -> this.costDAO.deleteCost(deliveryDriverId, uuid))
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
        String processName = "Tender With Created Status";
        log.logChecking(processName);
        return this.tenderDAO.getTender(tenderCode)
                .flatMap(tender -> {
                    if (!tender.status.equals(TenderDTO.StatusEnum.CREATED.toString())){
                        return Mono.error(new PnGenericException(typeException, typeException.getMessage()));
                    }
                    log.logCheckingOutcome(processName, tender.status.equals(TenderDTO.StatusEnum.CREATED.toString()), typeException.getMessage());
                    return Mono.just(tender);
                });
    }


    @Override
    public Mono<TenderCreateResponseDTO> updateStatusTender(String tenderCode, Status status) {
        return this.tenderDAO.getTender(tenderCode)
                .switchIfEmpty(Mono.error(new PnGenericException(TENDER_NOT_EXISTED, TENDER_NOT_EXISTED.getMessage())))
                .map(entity -> {
                    if (entity.getActualStatus().equals(TenderDTO.StatusEnum.IN_PROGRESS.getValue()) ||
                            entity.getActualStatus().equals(TenderDTO.StatusEnum.ENDED.getValue())) {
                        throw new PnGenericException(STATUS_NOT_VARIABLE, STATUS_NOT_VARIABLE.getMessage());
                    }
                    return entity;
                })
                .flatMap(entity -> {
                    if (!entity.getStatus().equalsIgnoreCase(status.getStatusCode().getValue()) &&
                            entity.getStatus().equalsIgnoreCase(TenderDTO.StatusEnum.CREATED.getValue())) {
                        return consolidateTender(entity);
                    }
                    return Mono.just(entity);
                })
                .flatMap(entity -> {
                    entity.setStatus(status.getStatusCode().getValue());
                    return this.tenderDAO.createOrUpdate(entity)
                            .map(modifyEntity -> {
                                TenderCreateResponseDTO response = new TenderCreateResponseDTO();
                                response.setTender(TenderMapper.tenderToDto(modifyEntity));
                                response.setCode(TenderCreateResponseDTO.CodeEnum.NUMBER_0);
                                response.setResult(true);
                                return response;
                            });
                });
    }


    private Mono<PnTender> consolidateTender(PnTender tender){
        String processName = "Consolidate Tender";
        log.logChecking(processName);
        return this.tenderDAO.getConsolidate(tender.getStartDate(), tender.getEndDate())
                .map(newTender -> true)
                .switchIfEmpty(Mono.just(false))
                .flatMap(existConsolidated -> {
                    if (Boolean.TRUE.equals(existConsolidated)){
                        log.logCheckingOutcome(processName, false, "exist Consolidate");
                        return Mono.error(new PnGenericException(CONSOLIDATE_ERROR, CONSOLIDATE_ERROR.getMessage()));
                    }
                    return isValidFSUCost(tender.getTenderCode());
                })
                .flatMap(isValidTender -> {
                    if (Boolean.FALSE.equals(isValidTender)) {
                        return Mono.error(new PnGenericException(FSUCOST_VALIDATOR_NOTVALID, FSUCOST_VALIDATOR_NOTVALID.getMessage()));
                    }
                    log.logCheckingOutcome(processName, true);
                    return Mono.just(tender);
                });
    }

    private Mono<Boolean> isValidFSUCost(String tenderCode){
        String processName = "Valid FSU Cost";
        log.logChecking(processName);
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
                .map(mapValidation -> {
                    log.logCheckingOutcome(processName, mapValidation.containsValue(true), "Empty Validation Cost");
                    return mapValidation.values().stream().filter(Boolean.FALSE::equals).toList().isEmpty();
                });
    }

}
