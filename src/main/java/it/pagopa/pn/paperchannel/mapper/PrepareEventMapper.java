package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.time.Instant;
import java.util.List;

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

    public static PrepareEvent toPrepareEvent(PnDeliveryRequest deliveryRequest, Address address, StatusCodeEnum status){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(deliveryRequest.getRequestId());
        entityEvent.setStatusCode(status);
        if (address != null){
            entityEvent.setReceiverAddress(AddressMapper.toPojo(address));
        }
        entityEvent.setStatusDetail(deliveryRequest.getStatusCode());
        entityEvent.setProductType(deliveryRequest.getProductType());
        entityEvent.setStatusDateTime(Instant.now());
        if (status == StatusCodeEnum.OK)
        {
            // vado a popolare eventuali url generati da f24, mi baso sul fatto che abbiano il generatedFrom popolato
            List<String> f24FileKeys = deliveryRequest.getAttachments().stream().filter(x -> x.getGeneratedFrom() != null).map(PnAttachmentInfo::getFileKey).toList();
            if (!f24FileKeys.isEmpty())
            {
                entityEvent.setReplacedF24AttachmentUrls(f24FileKeys);
            }
        }
        return entityEvent;
    }

    public static PrepareEvent toPrepareEvent(PnDeliveryRequest deliveryRequest, Address address, StatusCodeEnum status, KOReason koReason){
        PrepareEvent prepareEvent = toPrepareEvent(deliveryRequest, address, status);
        if(koReason != null) {
            prepareEvent.setFailureDetailCode(koReason.failureDetailCode());
            if(koReason.addressFailed() != null) {
                prepareEvent.setReceiverAddress(AddressMapper.toPojo(koReason.addressFailed()));
            }
        }
        return prepareEvent;
    }

}
