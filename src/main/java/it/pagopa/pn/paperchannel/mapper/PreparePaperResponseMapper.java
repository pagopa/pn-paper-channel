package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;

import java.util.Date;

public class PreparePaperResponseMapper {

    public static SendEvent fromResult(){
        SendEvent event = new SendEvent();
        event.setRequestId("abbcccc");
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
