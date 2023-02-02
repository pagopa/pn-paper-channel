package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;

import java.util.Date;

public class PreparePaperResponseMapper {

    private PreparePaperResponseMapper(){
        throw new IllegalCallerException();
    }

    public static PaperChannelUpdate fromResult(PnDeliveryRequest item, PnAddress pnAddress){
        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        if (hasPrepareStatusCode(item.getStatusCode())){
            paperChannelUpdate.setPrepareEvent(PrepareEventMapper.fromResult(item,pnAddress));
        } else {
            paperChannelUpdate.setSendEvent(SendEventMapper.fromResult(item,pnAddress));
        }
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

    private static boolean hasPrepareStatusCode(String statusCode) {
        boolean prepareCode = false;
        if (statusCode.equals(StatusDeliveryEnum.IN_PROCESSING.getCode())
                || statusCode.equals(StatusDeliveryEnum.TAKING_CHARGE.getCode())
                || statusCode.equals(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode())
                || statusCode.equals(StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR.getCode())
                || statusCode.equals(StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR.getCode())) {
            prepareCode = true;
        }
        return prepareCode;
    }

}
