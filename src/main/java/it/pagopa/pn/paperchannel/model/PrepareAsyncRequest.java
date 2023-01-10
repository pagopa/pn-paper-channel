package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class PrepareAsyncRequest {

    private String requestId;
    private String correlationId;
    private Address address;


}
