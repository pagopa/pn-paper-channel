package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;

public class PreparePaperResponseMapper {

    public static SendEvent fromResult(){
        SendEvent event = new SendEvent();
        event.setRequestId("abbcccc");
        return event;
    }

}
