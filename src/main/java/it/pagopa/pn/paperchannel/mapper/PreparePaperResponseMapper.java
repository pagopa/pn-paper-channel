package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;
import java.util.stream.Collectors;

public class PreparePaperResponseMapper {

    private static final BaseMapper <PnAddress,AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    private static final BaseMapper <PnAttachmentInfo, AttachmentDetails> baseMapperAttachment = new BaseMapperImpl(PnAttachmentInfo.class, AttachmentDetails.class);

    public static SendEvent fromResult(PnDeliveryRequest item){
        SendEvent event = new SendEvent();
        event.setRequestId(item.getRequestId());
        event.setStatusCode(item.getStatusCode());
        event.setStatusDetail(item.getStatusDetail());
        event.setStatusDateTime(DateUtils.parseDateString(item.getStatusDate()));
        event.setRegisteredLetterCode(item.getRegisteredLetterCode());
        event.setClientRequestTimeStamp(DateUtils.parseDateString(item.getStartDate()));


       // if(item.getAddress()!= null){
         //   event.setDiscoveredAddress(baseMapperAddress.toDTO(item.getAddress()));
       // }
        if(item.getAttachments()!= null){
            event.setAttachments(item.getAttachments().stream().map(baseMapperAttachment::toDTO).collect(Collectors.toList()));
        }
        return event;
    }

    public static PaperEvent fromEvent(String requestId){
        PaperEvent event = new PaperEvent();
        event.setRequestId(requestId);
        event.setStatusCode("PRESA_IN_CARICO");
        event.setStatusDetail("Presa in carico");
        event.setStatusDateTime(new Date());
        return event;
    }

}
