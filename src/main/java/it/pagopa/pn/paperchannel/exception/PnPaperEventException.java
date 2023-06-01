package it.pagopa.pn.paperchannel.exception;


import lombok.Getter;

@Getter
public class PnPaperEventException extends RuntimeException {

    private final String requestId;

    public PnPaperEventException(String requestId){
        this.requestId = requestId;
    }


}
