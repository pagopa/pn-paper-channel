package it.pagopa.pn.paperchannel.model;

import lombok.Getter;

@Getter
public enum StatusDeliveryEnum {
    IN_PROCESSING("PC000", "In elaborazione", Constants.PROGRESS),
    TAKING_CHARGE("PC001","Presa in carico", Constants.PROGRESS),
    DISCARD_NOTIFICATION("PC999","Discard Notification", Constants.KO),
    F24_WAITING("PC015", "In attesa di generazione PDF da F24", Constants.PROGRESS),
    F24_ERROR("PC016", "Errore su generazione PDF da F24", Constants.PROGRESS),
    NATIONAL_REGISTRY_WAITING("PC002", "In attesa di indirizzo da National Registry", Constants.PROGRESS),
    NATIONAL_REGISTRY_ERROR("PC005", "Errore con il recupero indirizzo da National Registry", Constants.PROGRESS),
    READY_TO_SEND("PC003","Pronto per l'invio", Constants.PROGRESS),
    UNTRACEABLE("PC010", "Irreperibile totale", Constants.KO),
    PRINTED("001", "Stampato", Constants.PROGRESS),
    DELIVERY_DRIVER_AVAILABLE("002", "Disponibile al recapitista", Constants.PROGRESS),
    DELIVERY_DRIVER_IN_CHARGE("003", "Preso in carico dal recapitista", Constants.PROGRESS),
    DELIVERED("004", "Consegnato", Constants.OK),
    DELIVERY_MISSING("005", "Mancata consegna", Constants.OK),
    LOST_DAMAGE("006", "Furto/Smarrimanto/deterioramento", Constants.OK),
    DELIVERED_POST_OFFICE("007", "Consegnato Ufficio Postale", Constants.OK),
    DELIVERY_MISSING_POST_OFFICE("008", "Mancata consegna Ufficio Postale", Constants.OK),
    IN_STOCK("009", "Compiuta giacenza", Constants.PROGRESS),
    PAPER_CHANNEL_NEW_REQUEST("PC001", "Paper channel nuova richiesta invio cartaceo, a valle di un fallimento temporaneo", Constants.KO),
    PAPER_CHANNEL_DEFAULT_ERROR("PC011", "Notifica in errore", Constants.KO),
    PAPER_CHANNEL_ASYNC_ERROR("PC012", "Errore nella fase di prepare", Constants.KO),
    SAFE_STORAGE_IN_ERROR("PC013", "Errore durante il recupero degli allegati", Constants.KO),
    DEFAULT_ERROR("PC014", "Errore", Constants.KO),
    DEDUPLICATES_ERROR_RESPONSE("PNALL001", "Normalizzazione con errore", Constants.PROGRESS);

    private final String code;
    private final String description;
    private final String detail;

    StatusDeliveryEnum(String code, String description,  String detail) {
        this.code = code;
        this.detail = detail;
        this.description = description;
    }

    private static class Constants {
        private static final String PROGRESS = "PROGRESS";
        private static final String OK = "OK";
        private static final String KO = "KO";
    }
}
