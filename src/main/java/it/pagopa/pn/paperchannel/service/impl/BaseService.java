package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
public class BaseService {

    protected final PnAuditLogBuilder auditLogBuilder;
    protected final NationalRegistryClient nationalRegistryClient;
    protected RequestDeliveryDAO requestDeliveryDAO;
    protected PnLogAudit pnLogAudit;

    public BaseService(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO, NationalRegistryClient nationalRegistryClient) {
        this.auditLogBuilder = auditLogBuilder;
        this.pnLogAudit = new PnLogAudit(auditLogBuilder);
        this.nationalRegistryClient = nationalRegistryClient;
        this.requestDeliveryDAO = requestDeliveryDAO;
    }

    protected Mono<Double> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType){
        if (StringUtils.isNotBlank(address.getCap())) {
            return getAmount(attachments, address.getCap(), productType)
                    .map(item -> item);
        }
        return getZone(address.getCountry())
                .flatMap(zone -> getAmount(attachments, zone, productType).map(item -> item));

    }


    protected void finderAddressFromNationalRegistries(String requestId, String fiscalCode, String personType){
        Mono.delay(Duration.ofMillis(20)).publishOn(Schedulers.boundedElastic())
                .flatMap(i -> {
                    log.debug("Start call national registries for find address");
                    return this.nationalRegistryClient.finderAddress(fiscalCode, personType);
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(address -> {
                    String correlationId = address.getCorrelationId();
                    log.info("National registries had response");
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
                    log.error("NationalRegistries finderaddress in errors");
                    log.error(ex.getMessage());
                    return Mono.empty();
                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    private Mono<String> getZone(String country) {
        return Mono.just("ZONA_1");
    }


    private Mono<Double> getPriceAttachments(List<AttachmentInfo> attachmentInfos, Double priceForAAr){
        return Flux.fromStream(attachmentInfos.stream())
                .map(attachmentInfo -> attachmentInfo.getNumberOfPage() * priceForAAr)
                .reduce(0.0, Double::sum);
    }


    private Mono<Contract> getContract(String capOrZone, ProductTypeEnum productType) {
        return Mono.just(new Contract(5.0, 10.0));
    }


    private Mono<Double> getAmount(List<AttachmentInfo> attachments, String capOrZone, ProductTypeEnum productType ){
        return getContract(capOrZone, productType)
                .flatMap(contract -> getPriceAttachments(attachments, contract.getPricePerPage())
                        .map(priceForPages -> Double.sum(contract.getPrice(), priceForPages))
                );

    }

}
