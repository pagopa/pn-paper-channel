package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
import lombok.Getter;

@Getter
public class PnPaperEventException extends RuntimeException{

    private final String requestId;

    public PnPaperEventException(String requestId){
        this.requestId = requestId;
    }


}
