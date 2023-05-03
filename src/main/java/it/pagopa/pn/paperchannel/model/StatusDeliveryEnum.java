package it.pagopa.pn.paperchannel.model;

import lombok.Getter;

@Getter
public enum StatusDeliveryEnum {
    IN_PROCESSING("PC000", "In elaborazione", "PROGRESS"),
    TAKING_CHARGE("PC001","Presa in carico", "PROGRESS"),
    NATIONAL_REGISTRY_WAITING("PC002", "In attesa di indirizzo da National Registry", "PROGRESS"),
    NATIONAL_REGISTRY_ERROR("PC005", "Errore con il recupero indirizzo da National Registry", "PROGRESS"),
    READY_TO_SEND("PC003","Pronto per l'invio", "PROGRESS"),
    UNTRACEABLE("PC010", "Irreperibile totale", "PROGRESS"),
    PRINTED("001", "Stampato", "PROGRESS"),
    DELIVERY_DRIVER_AVAILABLE("002", "Disponibile al recapitista", "PROGRESS"),
    DELIVERY_DRIVER_IN_CHARGE("003", "Preso in carico dal recapitista", "PROGRESS"),
    DELIVERED("004", "Consegnato", "OK"),
    DELIVERY_MISSING("005", "Mancata consegna", "OK"),
    LOST_DAMAGE("006", "Furto/Smarrimanto/deterioramento", "OK"),
    DELIVERED_POST_OFFICE("007", "Consegnato Ufficio Postale", "OK"),
    DELIVERY_MISSING_POST_OFFICE("008", "Mancata consegna Ufficio Postale", "OK"),
    IN_STOCK("009", "Compiuta giacenza", "PROGRESS"),
    PAPER_CHANNEL_NEW_REQUEST("PC001", "Paper channel nuova richiesta invio cartaceo, a valle di un fallimento temporaneo", "KO"),
    PAPER_CHANNEL_DEFAULT_ERROR("PC011", "Notifica in errore", "KO"),
    PAPER_CHANNEL_ASYNC_ERROR("PC012", "Errore nella fase di prepare", "KO"),
    SAFE_STORAGE_IN_ERROR("PC013", "Errore durante il recupero degli allegati", "KO"),
    DEFAULT_ERROR("PC014", "Errore", "KO");

    private final String code;
    private final String description;
    private final String detail;

    StatusDeliveryEnum(String code, String description,  String detail) {
        this.code = code;
        this.detail = detail;
        this.description = description;
    }
}
