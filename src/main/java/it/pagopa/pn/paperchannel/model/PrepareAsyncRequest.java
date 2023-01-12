package it.pagopa.pn.paperchannel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PrepareAsyncRequest {

    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("correlationId")
    private String correlationId;
    @JsonProperty("address")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Address address;

    public PrepareAsyncRequest(String requestId, String correlationId, Address address) {
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.address = address;
    }
}
