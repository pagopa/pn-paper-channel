package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestErrorCategoryEnum {
    RENDICONTAZIONE_SCARTATA("RENDICONTAZIONE_SCARTATA", "Rendicontazione scartata"),
    UNKNOWN("UNKNOWN", "Errore non categorizzato");

    private final String value;
    private final String description;
}
