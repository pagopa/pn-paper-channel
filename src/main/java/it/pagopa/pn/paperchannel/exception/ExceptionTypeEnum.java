package it.pagopa.pn.paperchannel.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum{
    CLIENT_ID_NOT_PRESENT("CLIENT_ID_NOT_PRESENT", "Client non censito."),
    CLIENT_ID_EMPTY("CLIENT_ID_EMPTY", "Non è stato inserito alcun valore per l'header clientID"),
    CLIENT_ID_NOT_IN_CONTEXT("CLIENT_ID_NOT_IN_CONTEXT", "Non è stato possibile recuperare il valore del client id nel contesto"),
    DELIVERY_REQUEST_NOT_EXIST("DELIVERY_REQUEST_NOT_EXIST", "La richiesta non esiste"),
    ADDRESS_NOT_EXIST("ADDRESS_NOT_EXIST", "L'indirizzo non è presente a DB"),
    ADDRESS_MANAGER_ERROR("ADDRESS_MANAGER_ERROR", "Problemi con l'indirizzo"),
    DELIVERY_REQUEST_IN_PROCESSING("DELIVERY_REQUEST_IN_PROCESSING", "La richiesta in elaborazione"),
    DATA_NULL_OR_INVALID("DATA_NULL_OR_INVALID", "La richiesta è vuota o contiene dati non validi per l'elaborazione"),
    MAPPER_ERROR("MAPPER_ERROR", "Non è stato possibile mappare l'oggetto richiesto"),
    PREPARE_ASYNC_LISTENER_EXCEPTION("PREPARE_ASYNC_LISTENER_EXCEPTION", "Si è verificato un errore durante la prepare async"),
    NATIONAL_REGISTRY_LISTENER_EXCEPTION("NATIONAL_REGISTRY_LISTENER_EXCEPTION", "Si è verificato un errore nel listener National Registry"),
    EXTERNAL_CHANNEL_LISTENER_EXCEPTION("EXTERNAL_CHANNEL_LISTENER_EXCEPTION", "Si è verificato un errore durante la RESULT di external channel"),
    EXTERNAL_CHANNEL_API_EXCEPTION("EXTERNAL_CHANNEL_API_EXCEPTION", "Si è verificato un errore durante l'invocazione a external channel"),
    NATIONAL_REGISTRY_ADDRESS_NOT_FOUND("NATIONAL_REGISTRY_ADDRESS_NOT_FOUND", "Non è stato trovato alcun indirizzo"),
    UNTRACEABLE_ADDRESS("UNTRACEABLE_ADDRESS", "Irreperibile totale"),
    CORRELATION_ID_NOT_FOUND("CORRELATION_ID_NOT_FOUND", "Non è stato possibile trovare il correlation id"),
    RETRY_AFTER_DOCUMENT("RETRY_AFTER_DOCUMENT", "Documento non disponibile al momento"),
    DOCUMENT_URL_NOT_FOUND("DOCUMENT_URL_NOT_FOUND", "Url allegato non disponibile"),
    DOCUMENT_NOT_DOWNLOADED("DOCUMENT_NOT_DOWNLOADED", "Non è stato possibile scaricare il documento"),
    DIFFERENT_DATA_REQUEST("DIFFERENT_DATA_REQUEST", "Richiesta già preso in carico ma sono state inviate informazioni differenti "),
    DIFFERENT_SEND_COST("DIFFERENT_SEND_COST", "Il costo in fase di invio differisce da quanto calcolato in fase di prepare e usato per generare gli f24"),
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
    TENDER_CANNOT_BE_DELETED("TENDER_CANNOT_BE_DELETED", "La gara non può essere eliminata."),
    DRIVER_CANNOT_BE_DELETED("DRIVER_CANNOT_BE_DELETED", "Il recapitista non può essere eliminato, appartiene ad una gara convalidata."),
    COST_CANNOT_BE_DELETED("COST_CANNOT_BE_DELETED", "Il costo non può essere eliminato, appartiene ad una gara convalidata."),
    DATA_VAULT_ENCRYPTION_ERROR("DATA_VAULT_ENCRYPTION_ERROR", "Servizio irraggiungibile od errore in fase di criptazione"),
    DATA_VAULT_DECRYPTION_ERROR("DATA_VAULT_DECRYPTION_ERROR", "Servizio irraggiungibile od errore in fase di decriptazione"),
    CONSOLIDATE_ERROR("CONSOLIDATE_ERROR", "non è possibile consolidare la Gara perchè ne esiste già una consolidata in questo range di date"),
    STATUS_NOT_VARIABLE("STATUS_NOT_VARIABLE", "Lo stato della Gara non può essere aggiornato"),
    FSUCOST_VALIDATOR_NOTVALID("FSUCOST_VALIDATOR_NOTVALID", "La gara non può essere consolidata, non sono stati definiti i costi di default per fsu"),
    INVALID_CAP_PRODUCT_TYPE("INVALID_CAP_PRODUCT_TYPE", "Sono presenti duplicati tra CAP e Product Type"),
    INVALID_CAP_FSU("INVALID_CAP_FSU", "Per FSU non sono stati inseriti i cap di default"),
    INVALID_ZONE_FSU("INVALID_ZONE_FSU", "Non sono stati inseriti i costi internazionali obbligatori per FSU"),
    INVALID_ZONE_PRODUCT_TYPE("INVALID_ZONE_PRODUCT_TYPE", "Ci sono Costi internazionali duplicati."),
    ATTEMPT_ADDRESS_NATIONAL_REGISTRY("ATTEMPT_ADDRESS_NATIONAL_REGISTRY", "Postman flow failed, find from national registry"),
    DISCARD_NOTIFICATION("DISCARD_NOTIFICATION", "Discard notification"),
    INVALID_VALUE_FROM_PROPS("INVALID_VALUE_FROM_PROPS", "Il valore della proprietà è diverso da quello atteso"),
    INVALID_SAFE_STORAGE("INVALID_SAFE_STORAGE", "Il Safe Storage selezionato è inesistente."),
    WRONG_EVENT_ORDER("WRONG_EVENT_ORDER", "Uno o più eventi precedenti non sono stati trovati"),
    WRONG_RECAG012_DATA("WRONG_RECAG012_DATA", "E' stato già trovato l'evento RECAG012 in precedenza, con dati differenti"),
    RESPONSE_NULL_FROM_DEDUPLICATION("RESPONSE_NULL_FROM_DEDUPLICATION", "La deduplication ha risposto con normalizedAddress null"),
    RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION("RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION", "La deduplication ha risposto con con un codice errore non gestito"),
    F24_ERROR("F24_ERROR", "Problemi con la generazione dei PDF F24"),
    COST_OUF_OF_RANGE("COST_OUF_OF_RANGE", "Il costo calcolato della notifica sfora il limite dell'ultimo range"),
    ERROR_CODE_PAPERCHANNEL_ZIP_HANDLE("PN_PAPERCHANNEL_ZIP_HANDLE", "Errore nel flusso di gestione dell'allegato ZIP");

    private final String title;
    private final String message;


    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }

}
