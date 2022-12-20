package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class DeliveryAsyncModel {

    private Address address;

    private List<AttachmentInfo> attachments;

    private Double amount;

    private String requestId;

    private String hashOldAddress;


}
