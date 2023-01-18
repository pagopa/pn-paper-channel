package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class BaseService {

    protected final PnAuditLogBuilder auditLogBuilder;

    public BaseService(PnAuditLogBuilder auditLogBuilder) {
        this.auditLogBuilder = auditLogBuilder;
    }

    protected Mono<Double> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType){
        if (StringUtils.isNotBlank(address.getCap())) {
            return getAmount(attachments, address.getCap(), productType)
                    .map(item -> item);
        }
        return getZone(address.getCountry())
                .flatMap(zone -> getAmount(attachments, zone, productType).map(item -> item));

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
