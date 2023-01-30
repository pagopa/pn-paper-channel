package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.ContractMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.ZoneDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
public class BaseService {

    protected final PnAuditLogBuilder auditLogBuilder;
    protected final NationalRegistryClient nationalRegistryClient;
    protected RequestDeliveryDAO requestDeliveryDAO;
    protected CostDAO costDAO;
    protected PnLogAudit pnLogAudit;

    @Autowired
    protected ZoneDAO zoneDAO;

    public BaseService(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO, CostDAO costDAO, NationalRegistryClient nationalRegistryClient) {
        this.auditLogBuilder = auditLogBuilder;
        this.pnLogAudit = new PnLogAudit(auditLogBuilder);
        this.nationalRegistryClient = nationalRegistryClient;
        this.requestDeliveryDAO = requestDeliveryDAO;
        this.costDAO = costDAO;
    }

    protected Mono<Double> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType){
        if (StringUtils.isNotBlank(address.getCap())) {
            return getAmount(attachments, address.getCap(), null, getProductType(address, productType))
                    .map(item -> item);
        }
        return getZone(address.getCountry())
                .flatMap(zone -> getAmount(attachments,null, zone, getProductType(address, productType)).map(item -> item));

    }


    void finderAddressFromNationalRegistries(String requestId, String fiscalCode, String personType, String iun){
        Mono.delay(Duration.ofMillis(20)).publishOn(Schedulers.boundedElastic())
                .flatMap(i -> {
                    log.info("Start call national registries for find address");
                    return this.nationalRegistryClient.finderAddress(fiscalCode, personType);
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(address -> {
                    String correlationId = address.getCorrelationId();
                    log.info("National registries has response");
                    return this.requestDeliveryDAO.getByRequestId(requestId)
                            .flatMap(entity -> {
                                log.debug("Entity edited with correlation id and new status");
                                pnLogAudit.addsSuccessResolveService(iun, String.format("prepare requestId = %s, trace_id = %s Response OK from National Registry service", requestId, MDC.get(MDC_TRACE_ID_KEY)));

                                entity.setCorrelationId(correlationId);
                                entity.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
                                entity.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
                                entity.setStatusDate(DateUtils.formatDate(new Date()));
                                return this.requestDeliveryDAO.updateData(entity).flatMap(Mono::just);
                            });
                })
                .flatMap(Mono::just)
                .onErrorResume(ex -> {
                    pnLogAudit.addsFailLog(iun, String.format("prepare requestId = %s, trace_id = %s Response KO from National Registry service", requestId, MDC.get(MDC_TRACE_ID_KEY)));

                    log.error("NationalRegistries finderaddress in errors");
                    log.error(ex.getMessage());
                    return Mono.empty();
                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    private Mono<String> getZone(String country) {
        //TODO decommentare quando la tabella sarà popolata
//        return zoneDAO.getByCountry(country)
//                .map(item -> item.getZone());
        return Mono.just("ZONA_1");
    }


    private Mono<Double> getPriceAttachments(List<AttachmentInfo> attachmentInfos, Double priceForAAr){
        return Flux.fromStream(attachmentInfos.stream())
                .map(attachmentInfo -> {
                    if (StringUtils.equals(attachmentInfo.getDocumentType(), PN_AAR)) {
                        return priceForAAr;
                    }
                    return attachmentInfo.getNumberOfPage() * priceForAAr;
                })
                .reduce(0.0, Double::sum);
    }


    private Mono<Contract> getContract(String cap, String zone, String productType) {
        // TODO decommentare quando la tabella sarà popolata
        Contract c = new Contract();
        c.setPrice(10.0);
        c.setPricePerPage(1.2);
        return Mono.just(c);
//        return costDAO.getByCapOrZoneAndProductType(cap, zone, productType).map(ContractMapper::toContract)
//                .onErrorResume(PnGenericException.class, ex -> {
//                    log.info("Cost not found try with default");
//                    return costDAO.getByCapOrZoneAndProductType(StringUtils.isNotEmpty(cap)? Const.CAP_DEFALUT : null, StringUtils.isNotEmpty(zone)? Const.ZONE_DEFAULT : null, productType)
//                            .map(ContractMapper::toContract);
//                });
    }


    private Mono<Double> getAmount(List<AttachmentInfo> attachments, String cap, String zone, String productType){
        return getContract(cap, zone, productType)
                .flatMap(contract -> getPriceAttachments(attachments, contract.getPricePerPage())
                        .map(priceForPages -> Double.sum(contract.getPrice(), priceForPages))
                );

    }

    private String getProductType(Address address, ProductTypeEnum productTypeEnum){
        String productType = "";

        if(StringUtils.isNotBlank(address.getCap())){
            if (productTypeEnum.equals(ProductTypeEnum.RN_AR)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RN_RS)){
                productType = RACCOMANDATA_SEMPLICE;
            } else if (productTypeEnum.equals(ProductTypeEnum.RN_890)){
                productType = RACCOMANDATA_890;
            }
        } else {
            if (productTypeEnum.equals(ProductTypeEnum.RI_AR)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RI_RS)){
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
                proposalProductType = ProductTypeEnum.RN_RS.getValue();
            }
            if(productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum.RN_890.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR)){
                proposalProductType = ProductTypeEnum.RN_AR.getValue();
            }
        }
        //internazionale
        else{
            if(productType.equals(RACCOMANDATA_SEMPLICE)){
                proposalProductType = ProductTypeEnum.RI_RS.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR) || productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum.RI_AR.getValue();
            }
        }
        return proposalProductType;
    }

}
