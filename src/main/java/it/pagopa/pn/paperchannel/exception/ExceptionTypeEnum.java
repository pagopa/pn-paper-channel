package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum{
    DELIVERY_REQUEST_NOT_EXIST("DELIVERY_REQUEST_NOT_EXIST", "La richiesta non esiste"),
    DELIVERY_REQUEST_IN_PROCESSING("DELIVERY_REQUEST_IN_PROCESSING", "La richiesta in elaborazione"),
    DATA_NULL_OR_INVALID("DATA_NULL_OR_INVALID", "La richiesta è vuota o contiene dati non validi per l'elaborazione"),
    MAPPER_ERROR("MAPPER_ERROR", "Non è stato possibile mappare l'oggetto richiesto"),
    PREPARE_ASYNC_LISTENER_EXCEPTION("PREPARE_ASYNC_LISTENER_EXCEPTION", "Si è verificato un errore durante la prepare async"),
    EXTERNAL_CHANNEL_LISTENER_EXCEPTION("EXTERNAL_CHANNEL_LISTENER_EXCEPTION", "Si è verificato un errore durante la RESULT di external channel"),
    EXTERNAL_CHANNEL_API_EXCEPTION("EXTERNAL_CHANNEL_API_EXCEPTION", "Si è verificato un errore durante l'invocazione a external channel"),
    NATIONAL_REGISTRY_ADDRESS_NOT_FOUND("NATIONAL_REGISTRY_ADDRESS_NOT_FOUND", "Non è stato trovato alcun indirizzo"),
    UNTRACEABLE_ADDRESS("UNTRACEABLE_ADDRESS", "Irreperibile totale"),
    CORRELATION_ID_NOT_FOUND("CORRELATION_ID_NOT_FOUND", "Non è stato possibile trovare il correlation id"),
    RETRY_AFTER_DOCUMENT("RETRY_AFTER_DOCUMENT", "Documento non disponibile al momento"),
    DOCUMENT_URL_NOT_FOUND("DOCUMENT_URL_NOT_FOUND", "Url allegato non disponibile"),
    DOCUMENT_NOT_DOWNLOADED("DOCUMENT_NOT_DOWNLOADED", "Non è stato possibile scaricare il documento"),
    DIFFERENT_DATA_REQUEST("DIFFERENT_DATA_REQUEST", "Richiesta già preso in carico ma sono state inviate informazioni differenti "),
    COUNTRY_NOT_FOUND("COUNTRY NOT FOUND", "Il paese non è stato trovato"),
    COST_NOT_FOUND("COST NOT FOUND", "Il costo non è stato trovato"),
    DIFFERENT_DATA_RESULT("DIFFERENT_DATA_RESULT", "I dati restituiti dal servizio External Channel sono differenti rispetto a quelli memorizzati nel database (per uno stesso requestId)"),
    BADLY_FILTER_REQUEST("BADLY_FILTER_REQUEST", "I filtri contengono valori non corretti"),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "Il file non è stato caricato o non esiste"),
    EXCEL_BADLY_FORMAT("EXCEL_BADLY_FORMAT", "Il file è formattato male"),
    EXCEL_BADLY_CONTENT("EXCEL_BADLY_CONTENT", "Il file è corrotto"),
    BADLY_REQUEST("BADLY_REQUEST", "Campi obbligatori mancanti"),
    BADLY_DATE_INTERVAL("BADLY_DATE_INTERVAL", "L'intervallo di data è errato"),
    LISTENER_QUEUE_NOT_EXPIRED("LISTENER_QUEUE_NOT_EXPIRED", "Evento non ancora disponibile"),
    TENDER_NOT_EXISTED("TENDER_NOT_EXISTED", "La gara non è presente nel sistema"),
    ACTIVE_TENDER_NOT_FOUND("ACTIVE_TENDER_NOT_FOUND", "Errore, non esistono gare in corso"),
    DELIVERY_DRIVER_NOT_EXISTED("DELIVERY_DRIVER_NOT_EXISTED", "Il recapitisca non è presente nel sistema"),
    DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE("DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE", "Il recapitisca con questa partita iva è già presente con ruolo differente"),
    COST_ALREADY_EXIST("COST_ALREADY_EXIST", "Il costo è già presente tra i recapitisti."),
    COST_BADLY_CONTENT("COST_BADLY_CONTENT", "Informazioni errate per il costo"),
    COST_DRIVER_OR_FSU_NOT_FOUND("COST_DRIVER_OR_FSU_NOT_FOUND", "Il costo per il seguente prodotto non è presente tra i recapitisti e nel FSU"),
    FILE_REQUEST_ASYNC_NOT_FOUND("FILE_REQUEST_ASYNC_NOT_FOUND", "File non trovato, flusso asincrono"),
    DATA_VAULT_ENCRYPTION_ERROR("DATA_VAULT_ENCRYPTION_ERROR", "Servizio irraggiungibile od errore in fase di criptazione"),
    CONSOLIDATE_ERROR("CONSOLIDATE_ERROR", "La Gara è già in stato consolidato");

    private final String title;
    private final String message;


    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }

}
