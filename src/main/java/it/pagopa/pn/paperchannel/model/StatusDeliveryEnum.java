package it.pagopa.pn.paperchannel.model;

import lombok.Getter;

@Getter
public enum StatusDeliveryEnum {
    IN_PROCESSING("PC000", "In elaborazione"),
    TAKING_CHARGE("PC001","Presa in carico"),
    NATIONAL_REGISTRY_WAITING("PC002", "In attesa di indirizzo da National Registry"),
    NATIONAL_REGISTRY_ERROR("PC005", "Errore con il recupero indirizzo da National Registry"),
    READY_TO_SEND("PC003","Pronto per l'invio"),
    UNTRACEABLE("PC010", "Irreperibile totale"),
    PRINTED("001", "Stampato"),
    DELIVERY_DRIVER_AVAILABLE("002", "Disponibile al recapitista"),
    DELIVERY_DRIVER_IN_CHARGE("003", "Preso in carico dal recapitista"),
    DELIVERED("004", "Consegnato"),
    DELIVERY_MISSING("005", "Mancata consegna"),
    LOST_DAMAGE("006", "Furto/Smarrimanto/deterioramento"),
    DELIVERED_POST_OFFICE("007", "Consegnato Ufficio Postale"),
    DELIVERY_MISSING_POST_OFFICE("008", "Mancata consegna Ufficio Postale"),
    IN_STOCK("009", "Compiuta giacenza"),
    PAPER_CHANNEL_NEW_REQUEST("PC001", "Paper channel nuova richiesta invio cartaceo, a valle di un fallimento temporaneo"),
    PAPER_CHANNEL_DEFAULT_ERROR("PC011", "Notifica in errore"),
    PAPER_CHANNEL_ASYNC_ERROR("PC012", "Errore nella fase di prepare"),
    SAFE_STORAGE_IN_ERROR("PC013", "Errore durante il recupero degli allegati"),

    ;

    private final String code;
    private final String description;

    StatusDeliveryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
