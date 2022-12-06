package it.pagopa.pn.paperchannel.queue.model;

import it.pagopa.pn.paperchannel.pojo.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPayload {

    private Address deliveryAddress;

    private Double totalPrice;
}
