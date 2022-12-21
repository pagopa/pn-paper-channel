package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;

import java.util.Date;

public class PreparePaperResponseMapper {

    private static final BaseMapper <PnAddress,AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    private static final BaseMapper <PnAttachmentInfo, AttachmentDetails> baseMapperAttachment = new BaseMapperImpl(PnAttachmentInfo.class, AttachmentDetails.class);

    public static PaperChannelUpdate fromResult(PnDeliveryRequest item, PnAddress pnAddress){
        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setPrepareEvent(PrepareEventMapper.fromResult(item,pnAddress));

        return paperChannelUpdate;
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
