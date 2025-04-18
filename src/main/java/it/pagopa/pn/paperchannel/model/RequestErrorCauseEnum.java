package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestErrorCauseEnum {
    GIACENZA_DATE_ERROR("GIACENZA_DATE_ERROR", "Date di inizio e fine giacenza non coerenti"),
    REFINEMENT_DATE_ERROR("REFINEMENT_DATE_ERROR", "Possibile perfezionamento in data futura"),
    UNKNOWN("UNKNOWN", "Causa errore sconosciuta");

    private final String value;
    private final String description;
}
