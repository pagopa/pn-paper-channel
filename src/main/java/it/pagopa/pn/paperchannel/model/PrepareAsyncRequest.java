package it.pagopa.pn.paperchannel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrepareAsyncRequest {

    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("correlationId")
    private String correlationId;
    @JsonProperty("address")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Address address;

    @JsonProperty("isSecondAttempt")
    private boolean isSecondAttempt;

    @JsonProperty("attempt")
    private Integer attemptRetry;

}
