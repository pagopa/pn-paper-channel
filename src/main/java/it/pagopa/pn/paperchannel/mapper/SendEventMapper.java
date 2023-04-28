package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SendEventMapper {

    private SendEventMapper(){
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    public static SendEvent fromResult(PnDeliveryRequest request, PnAddress address){
        SendEvent entityEvent = new SendEvent();
        entityEvent.setRequestId(request.getRequestId());
        try {
            entityEvent.setStatusCode(StatusCodeEnum.valueOf(request.getStatusDetail()));
        } catch (IllegalArgumentException ex) {
            log.info("status code not found"+request.getStatusCode());
        }
        entityEvent.setStatusDescription(request.getStatusDescription());
        entityEvent.setStatusDetail(request.getStatusCode());
        entityEvent.setRegisteredLetterCode(request.getProductType());
        entityEvent.setStatusDateTime((DateUtils.parseDateString(request.getStatusDate())));
        entityEvent.setAttachments(request.getAttachments().stream().map(AttachmentMapper::toAttachmentDetails).toList());
        if (address != null && address.getTtl() != null) {
            entityEvent.setDiscoveredAddress(baseMapperAddress.toDTO(address));
        }
        return entityEvent;
    }
}
