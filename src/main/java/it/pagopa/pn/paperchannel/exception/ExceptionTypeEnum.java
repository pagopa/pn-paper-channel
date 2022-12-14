package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum{
    DELIVERY_REQUEST_NOT_EXIST("DELIVERY_REQUEST_NOT_EXIST", "La richiesta non esiste"),
    DELIVERY_REQUEST_IN_PROCESSING("DELIVERY_REQUEST_IN_PROCESSING", "La richiesta in elaborazione"),
    DATA_NULL_OR_INVALID("DATA_NULL_OR_INVALID", "La richiesta è vuota o contiene dati non validi per l'elaborazione"),
    MAPPER_ERROR("MAPPER_ERROR", "Non è stato possibile mappare l'oggetto richiesto"),
    PREPARE_ASYNC_LISTENER_EXCEPTION("PREPARE_ASYNC_LISTENER_EXCEPTION", "Si è verificato un errore durante la prepare async"),
    UNTRACEABLE_ADDRESS("UNTRACEABLE_ADDRESS", "Irreperibile totale"),
    RETRY_AFTER_DOCUMENT("RETRY_AFTER_DOCUMENT", "Documento non disponibile al momento"),
    DOCUMENT_URL_NOT_FOUND("DOCUMENT_URL_NOT_FOUND", "Url allegato non disponibile"),
    DOCUMENT_NOT_DOWNLOADED("DOCUMENT_NOT_DOWNLOADED", "Non è stato possibile scaricare il documento"),
    DIFFERENT_DATA_REQUEST("DIFFERENT_DATA_REQUEST", "Richiesta già preso in carico ma sono state inviate informazioni differenti (requestId già presente)"),
    COUNTRY_NOT_FOUND("COUNTRY NOT FOUND", "Il paese non è stato trovato"),
    COST_NOT_FOUND("COST NOT FOUND", "Il costo non è stato trovato"),
    DIFFERENT_DATA_RESULT("DIFFERENT_DATA_RESULT", "I dati restituiti dal servizio External Channel sono differenti rispetto a quelli memorizzati nel database (per uno stesso requestId)");

    private final String title;
    private final String message;


    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }

}
