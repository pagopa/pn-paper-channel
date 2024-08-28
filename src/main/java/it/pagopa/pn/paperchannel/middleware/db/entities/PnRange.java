package it.pagopa.pn.paperchannel.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import java.io.Serializable;
import java.math.BigDecimal;


@Getter
@Setter
@ToString
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class PnRange implements Serializable {
    private BigDecimal cost;
    private Integer minWeight;
    private Integer maxWeight;
}
