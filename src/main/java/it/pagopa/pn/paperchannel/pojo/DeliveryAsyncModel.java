package it.pagopa.pn.paperchannel.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeliveryAsyncModel {

    private Address address;

    private List<AttachmentInfo> attachments;

    private Double amount;

    private String requestId;


}
