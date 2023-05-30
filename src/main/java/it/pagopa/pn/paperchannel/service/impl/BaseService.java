package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.function.Function;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_ERROR;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING;
import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
public class BaseService {

    protected final PnAuditLogBuilder auditLogBuilder;
    protected final NationalRegistryClient nationalRegistryClient;
    protected final SqsSender sqsSender;
    protected RequestDeliveryDAO requestDeliveryDAO;
    protected CostDAO costDAO;
    public static final String MDC_TRACE_ID_KEY = "trace_id";
    @Autowired
    @Qualifier("dataVaultEncryption")
    protected DataEncryption dataEncryption;
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
                                log.debug("Entity edited with correlation id {} and new status {}", correlationId, NATIONAL_REGISTRY_WAITING.getDetail());
                                entity.setCorrelationId(correlationId);
                                return changeStatusDeliveryRequest(entity, NATIONAL_REGISTRY_WAITING);
                            });
                })
                .flatMap(Mono::just)
                .onErrorResume(ex -> {
                    pnLogAudit.addsWarningResolveService(iun, String.format("prepare requestId = %s, relatedRequestId = %s, trace_id = %s Response KO from National Registry service", requestId, relatedRequestId, null));
                    log.warn("NationalRegistries finder address in errors {}", ex.getMessage());
                    return Mono.empty();

                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    private Mono<PnDeliveryRequest> changeStatusDeliveryRequest(PnDeliveryRequest deliveryRequest, StatusDeliveryEnum status){
        RequestDeliveryMapper.changeState(
                deliveryRequest,
                status.getCode(),
                status.getDescription(),
                status.getDetail(),
                deliveryRequest.getProductType(), null);
        return this.requestDeliveryDAO.updateData(deliveryRequest).flatMap(Mono::just);
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




    protected String getProductType(Address address, ProductTypeEnum productTypeEnum){
        String productType = "";
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "it") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italia") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italy");

        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            if (productTypeEnum.equals(ProductTypeEnum.AR)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RS)){
                productType = RACCOMANDATA_SEMPLICE;
            } else if (productTypeEnum.equals(ProductTypeEnum._890)){
                productType = RACCOMANDATA_890;
            }
        } else {
            if (productTypeEnum.equals(ProductTypeEnum.RIR) || productTypeEnum.equals(ProductTypeEnum._890)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RIS)){
                productType = RACCOMANDATA_SEMPLICE;
            }
        }
        return productType;
    }

    protected String getProposalProductType(Address address, String productType){
        String proposalProductType = "";
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "it") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italia") ||
                StringUtils.equalsIgnoreCase(address.getCountry(), "italy");
        //nazionale
        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
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
