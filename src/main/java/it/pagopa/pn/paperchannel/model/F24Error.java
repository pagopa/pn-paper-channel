package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class F24Error implements Serializable {
    private String message;
    private String requestId;
    private String relatedRequestId;
    private String iun;
    private String correlationId;
    private int attempt;

}
