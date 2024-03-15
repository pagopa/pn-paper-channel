package it.pagopa.pn.paperchannel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PrepareAsyncRequest {

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

    @JsonProperty("isF24Flow")
    private boolean isF24ResponseFlow = false;

    @JsonProperty("attempt")
    private Integer attemptRetry;
    @JsonProperty("clientId")
    private String clientId;


    // Constructor used only national registry listener
    public PrepareAsyncRequest(String requestId, String correlationId, Address address) {
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.address = address;
    }


    public PrepareAsyncRequest(String requestId, String iun, boolean isAddressRetry, Integer attemptRetry) {
        this.requestId = requestId;
        this.iun = iun;
        this.isAddressRetry = isAddressRetry;
        this.attemptRetry = attemptRetry;
    }

    public PrepareAsyncRequest(String requestId, String iun, String correlationId, Address address, boolean isAddressRetry, Integer attemptRetry){
        this.requestId = requestId;
        this.iun = iun;
        this.correlationId = correlationId;
        this.address = address;
        this.isAddressRetry = isAddressRetry;
        this.attemptRetry = attemptRetry;
    }

}
