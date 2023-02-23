package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_ERROR;
import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
public class BaseService {

    protected final PnAuditLogBuilder auditLogBuilder;
    protected final NationalRegistryClient nationalRegistryClient;
    protected final SqsSender sqsSender;
    protected RequestDeliveryDAO requestDeliveryDAO;
    protected CostDAO costDAO;
    protected PnLogAudit pnLogAudit;

    public BaseService(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO, CostDAO costDAO,
                NationalRegistryClient nationalRegistryClient, SqsSender sqsSender) {
        this.auditLogBuilder = auditLogBuilder;
        this.pnLogAudit = new PnLogAudit(auditLogBuilder);
        this.nationalRegistryClient = nationalRegistryClient;
        this.requestDeliveryDAO = requestDeliveryDAO;
        this.sqsSender = sqsSender;
        this.costDAO = costDAO;
    }


    protected void finderAddressFromNationalRegistries(String correlationId, String requestId, String relatedRequestId, String fiscalCode, String personType, String iun, Integer attempt){
        Mono.delay(Duration.ofMillis(20)).publishOn(Schedulers.boundedElastic())
                .flatMap(i -> {
                    log.info("Start call national registries for find address");
                    pnLogAudit.addsBeforeResolveService(iun, String.format("prepare requestId = %s, relatedRequestId= %s, trace_id = %s Request to National Registry service", requestId, relatedRequestId, correlationId));
                    return this.nationalRegistryClient.finderAddress(correlationId, fiscalCode, personType)
                            .onErrorResume(e -> {
                                NationalRegistryError error = new NationalRegistryError();
                                error.setIun(iun);
                                error.setMessage(e.getMessage());
                                error.setFiscalCode(fiscalCode);
                                error.setReceiverType(personType);
                                error.setRequestId(requestId);
                                error.setRelatedRequestId(relatedRequestId);
                                saveErrorAndPushError(requestId, NATIONAL_REGISTRY_ERROR, error, payload -> {
                                    sqsSender.pushInternalError(payload, attempt, NationalRegistryError.class);
                                    return null;
                                });
                                return Mono.error(e);
                            });
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(address -> {
                    log.info("National registries has response");
                    return this.requestDeliveryDAO.getByRequestId(requestId)
                            .flatMap(entity -> {
                                log.debug("Entity edited with correlation id and new status");
                                
                                entity.setCorrelationId(correlationId);
                                entity.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
                                entity.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
                                entity.setStatusDate(DateUtils.formatDate(new Date()));
                                return this.requestDeliveryDAO.updateData(entity).flatMap(Mono::just);
                            });
                })
                .flatMap(Mono::just)
                .onErrorResume(ex -> {
                    pnLogAudit.addsFailResolveService(iun, String.format("prepare requestId = %s, relatedRequestId = %s, trace_id = %s Response KO from National Registry service", requestId, relatedRequestId, MDC.get(MDC_TRACE_ID_KEY)));

                    log.error("NationalRegistries finder address in errors");
                    log.error(ex.getMessage());
                    return Mono.empty();

                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    private Mono<PnDeliveryRequest> changeStatusDeliveryRequest(PnDeliveryRequest pnDeliveryRequeste, StatusDeliveryEnum status){
        pnDeliveryRequeste.setStatusCode(status.getCode());
        pnDeliveryRequeste.setStatusDetail(status.getDescription());
        pnDeliveryRequeste.setStatusDate(DateUtils.formatDate(new Date()));
        return this.requestDeliveryDAO.updateData(pnDeliveryRequeste).flatMap(Mono::just);
    }

    private <T> void saveErrorAndPushError(String requestId, StatusDeliveryEnum status,  T error, Function<T, Void> queuePush){
        this.requestDeliveryDAO.getByRequestId(requestId).publishOn(Schedulers.boundedElastic())
                .flatMap(entity -> changeStatusDeliveryRequest(entity, status)
                        .flatMap(updated -> {
                            queuePush.apply(error);
                            return Mono.just("");
                        })
                ).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    protected Mono<Double> getPriceAttachments(List<AttachmentInfo> attachmentInfos, Float priceForAAr){
        return Flux.fromStream(attachmentInfos.stream())
                .map(attachmentInfo -> {
                    if (StringUtils.equals(attachmentInfo.getDocumentType(), PN_AAR)) {
                        return priceForAAr;
                    }
                    return attachmentInfo.getNumberOfPage() * priceForAAr;
                })
                .reduce(0.0, Double::sum);
    }




    protected String getProductType(Address address, ProductTypeEnum productTypeEnum){
        String productType = "";

        if(StringUtils.isNotBlank(address.getCap())){
            if (productTypeEnum.equals(ProductTypeEnum.AR)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RS)){
                productType = RACCOMANDATA_SEMPLICE;
            } else if (productTypeEnum.equals(ProductTypeEnum._890)){
                productType = RACCOMANDATA_890;
            }
        } else {
            if (productTypeEnum.equals(ProductTypeEnum.RIS) || productTypeEnum.equals(ProductTypeEnum._890)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RIS)){
                productType = RACCOMANDATA_SEMPLICE;
            }
        }
        return productType;
    }

    protected String getProposalProductType(Address address, String productType){
        String proposalProductType = "";
        //nazionale
        if(StringUtils.isNotBlank(address.getCap())){
            if(productType.equals(RACCOMANDATA_SEMPLICE)){
                proposalProductType = ProductTypeEnum.RS.getValue();
            }
            if(productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum._890.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR)){
                proposalProductType = ProductTypeEnum.AR.getValue();
            }
        }
        //internazionale
        else {
            if(productType.equals(RACCOMANDATA_SEMPLICE)){
                proposalProductType = ProductTypeEnum.RIS.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR) || productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum.RIR.getValue();
            }
        }
        return proposalProductType;
    }

}
