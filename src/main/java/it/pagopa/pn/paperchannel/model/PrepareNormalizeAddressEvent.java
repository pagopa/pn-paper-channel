package it.pagopa.pn.paperchannel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Model representing the input payload of PREPARE phase 1.
 */
@Data
@Builder
public class PrepareNormalizeAddressEvent {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("iun")
    private String iun;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("address")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Address address;

    @JsonProperty("isAddressRetry")
    private boolean isAddressRetry = false;

    @JsonProperty("attempt")
    private Integer attemptRetry;

    @JsonProperty("clientId")
    private String clientId;
}
