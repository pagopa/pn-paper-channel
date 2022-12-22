package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class DeliveryAsyncModel {

    private boolean fromNationalRegistry = false;
    private Address address;

    private List<AttachmentInfo> attachments;

    private Double amount;

    private String requestId;

    private String hashOldAddress;

    private ProductTypeEnum productType;


}
