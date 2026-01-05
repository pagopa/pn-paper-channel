package it.pagopa.pn.paperchannel.model;

import lombok.Data;

@Data
public class PaperAddressRequestInternalDto {

    private String requestId;
    private Integer attempt;
    private Integer pcRetry;
    private Integer recIndex;

    private Address address;
    private boolean normalized;
}
