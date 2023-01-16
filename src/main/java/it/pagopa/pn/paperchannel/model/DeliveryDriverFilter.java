package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Getter
@Setter
@AllArgsConstructor
public class DeliveryDriverFilter {
    private Integer page;
    private Integer size;
    private Boolean status;
    private Date startDate;
    private Date endDate;
}
