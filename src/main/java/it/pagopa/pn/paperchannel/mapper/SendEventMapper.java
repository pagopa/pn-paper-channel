package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class SendEventMapper {

    private SendEventMapper(){
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl<>(PnAddress.class, AnalogAddress.class);

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
        entityEvent.setStatusDateTime((DateUtils.parseStringTOInstant(request.getStatusDate())));
        entityEvent.setAttachments(request.getAttachments().stream().map(AttachmentMapper::toAttachmentDetails).toList());
        if (address != null && address.getTtl() != null) {
            entityEvent.setDiscoveredAddress(baseMapperAddress.toDTO(address));
        }
        return entityEvent;
    }

    public static PnDeliveryRequest changeToProgressStatus(PnDeliveryRequest entity){
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        return entity;
    }

//    public static SendEvent createSendEventMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
//        SendEvent sendEvent = new SendEvent();
//        sendEvent.setStatusCode(StatusCodeEnum.valueOf(entity.getStatusDetail()));
//        sendEvent.setStatusDetail(paperRequest.getStatusCode());
//        sendEvent.setStatusDescription(entity.getStatusDescription());
//
//
//        sendEvent.setRequestId(entity.getRequestId());
//        sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(paperRequest.getStatusDateTime()));
//        sendEvent.setRegisteredLetterCode(paperRequest.getRegisteredLetterCode());
//        if (paperRequest.getClientRequestTimeStamp() != null) {
//            sendEvent.setClientRequestTimeStamp(paperRequest.getClientRequestTimeStamp().toInstant());
//        }
//
//        sendEvent.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
//        sendEvent.setDiscoveredAddress(AddressMapper.toPojo(paperRequest.getDiscoveredAddress()));
//
//        if (paperRequest.getAttachments() != null && !paperRequest.getAttachments().isEmpty()) {
//            sendEvent.setAttachments(
//                    paperRequest.getAttachments()
//                            .stream()
//                            .map(AttachmentMapper::fromAttachmentDetailsDto)
//                            .toList()
//            );
//        }
//
//        return sendEvent;
//
//    }
    public static SendEvent createSendEventMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        DematInternalEvent dematZipInternalEvent = DematInternalEventMapper.toDematInternalEvent(entity, paperRequest);
        return createSendEventMessage(dematZipInternalEvent);

    }

    public static SendEvent createSendEventMessage(DematInternalEvent dematZipInternalEvent) {
        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode(StatusCodeEnum.valueOf(dematZipInternalEvent.getStatusDetail()));
        sendEvent.setStatusDetail(dematZipInternalEvent.getStatusCode());
        sendEvent.setStatusDescription(dematZipInternalEvent.getStatusDescription());


        sendEvent.setRequestId(dematZipInternalEvent.getRequestId());
        sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(dematZipInternalEvent.getStatusDateTime()));
        sendEvent.setRegisteredLetterCode(dematZipInternalEvent.getRegisteredLetterCode());
        if (dematZipInternalEvent.getClientRequestTimeStamp() != null) {
            sendEvent.setClientRequestTimeStamp(dematZipInternalEvent.getClientRequestTimeStamp().toInstant());
        }

        sendEvent.setDeliveryFailureCause(dematZipInternalEvent.getDeliveryFailureCause());
        sendEvent.setDiscoveredAddress(dematZipInternalEvent.getDiscoveredAddress());

        sendEvent.setAttachments(List.of(dematZipInternalEvent.getAttachmentDetails()));

        return sendEvent;

    }

}
