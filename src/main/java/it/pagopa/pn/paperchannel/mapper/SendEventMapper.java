package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.stream.Collectors;

public class SendEventMapper {
    private static final BaseMapper<PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    public static SendEvent fromResult(PnDeliveryRequest request, PnAddress address){
        SendEvent entityEvent = new SendEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(request.getStatusCode());
        entityEvent.setStatusDetail(request.getStatusDetail());
        entityEvent.setRegisteredLetterCode(request.getProposalProductType());
        entityEvent.setStatusDateTime((DateUtils.parseDateString(request.getStatusDate())));
        entityEvent.setAttachments(request.getAttachments().stream().map(AttachmentMapper::toAttachmentDetails).collect(Collectors.toList()));
        return entityEvent;
    }
}
