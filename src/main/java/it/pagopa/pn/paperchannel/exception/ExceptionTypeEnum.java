package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum{
    DELIVERY_REQUEST_NOT_EXIST("DELIVERY_REQUEST_NOT_EXIST", "La richiesta non esiste");

    private final String title;
    private final String message;


    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }

}
