package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NationalRegistryError {
    private String message;
    private String requestId;
    private String fiscalCode;
    private String receiverType;
    private String iun;


}
