package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;

import java.util.Date;

public class PreparePaperResponseMapper {


    private PreparePaperResponseMapper(){
        throw new IllegalCallerException();
    }


    public static PaperChannelUpdate fromResult(PnDeliveryRequest item, PnAddress pnAddress){
        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        if (item.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode()) || item.getStatusCode().equals(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode())){
            paperChannelUpdate.setPrepareEvent(PrepareEventMapper.fromResult(item,pnAddress));
            return paperChannelUpdate;
        }
        paperChannelUpdate.setSendEvent(SendEventMapper.fromResult(item,pnAddress));

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
