package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@RequiredArgsConstructor
public abstract class SendToDeliveryPushHandler implements MessageHandler {

    private final SqsSender sqsSender;

    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        SendEvent sendEvent = createSendEventMessage(entity, paperRequest);
        sqsSender.pushSendEvent(sendEvent);
    }

    protected SendEvent createSendEventMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode(StatusCodeEnum.valueOf(entity.getStatusCode()));
        sendEvent.setStatusDetail(entity.getStatusDetail());
        sendEvent.setStatusDescription(entity.getStatusDetail());


        sendEvent.setRequestId(entity.getRequestId());
        sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(paperRequest.getStatusDateTime()));
        sendEvent.setRegisteredLetterCode(paperRequest.getRegisteredLetterCode());
        sendEvent.setClientRequestTimeStamp(Date.from(paperRequest.getClientRequestTimeStamp().toInstant()));
        sendEvent.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
        sendEvent.setDiscoveredAddress(AddressMapper.toPojo(paperRequest.getDiscoveredAddress()));

        if (paperRequest.getAttachments() != null && !paperRequest.getAttachments().isEmpty()) {
            sendEvent.setAttachments(
                    paperRequest.getAttachments()
                            .stream()
                            .map(AttachmentMapper::fromAttachmentDetailsDto)
                            .toList()
            );
        }

        return sendEvent;

    }

}
