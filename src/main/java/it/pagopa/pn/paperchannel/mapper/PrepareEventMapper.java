package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.time.Instant;

public class PrepareEventMapper {

    private PrepareEventMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper <PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl<>(PnAddress.class, AnalogAddress.class);

    public static PrepareEvent fromResult(PnDeliveryRequest request, PnAddress address){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(StatusCodeEnum.fromValue(request.getStatusDetail()));

        if (address != null && address.getTtl() != null){
           entityEvent.setReceiverAddress(baseMapperAddress.toDTO(address));
        }

        entityEvent.setStatusDetail(request.getStatusCode());
        entityEvent.setProductType(request.getProductType());
        entityEvent.setStatusDateTime((DateUtils.parseStringTOInstant(request.getStatusDate())));
        return entityEvent;
    }

    public static PrepareEvent toPrepareEvent(PnDeliveryRequest deliveryRequest, Address address, StatusCodeEnum status, KOReason koReason){
        FailureDetailCodeEnum failureDetailCode = koReason != null ? koReason.failureDetailCode() : null;
        Address addressFailed = koReason != null ? koReason.addressFailed() : null;
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(deliveryRequest.getRequestId());
        entityEvent.setStatusCode(status);
        if(addressFailed != null) {
            entityEvent.setReceiverAddress(AddressMapper.toPojo(koReason.addressFailed()));
        }
        else if (address != null){
            entityEvent.setReceiverAddress(AddressMapper.toPojo(address));
        }
        entityEvent.setStatusDetail(deliveryRequest.getStatusCode());
        entityEvent.setProductType(deliveryRequest.getProductType());
        entityEvent.setStatusDateTime(Instant.now());
        entityEvent.setFailureDetailCode(failureDetailCode);
        return entityEvent;
    }

}
