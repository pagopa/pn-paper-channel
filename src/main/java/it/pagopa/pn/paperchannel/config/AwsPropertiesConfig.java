package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("aws")
@Getter
@Setter
public class AwsPropertiesConfig extends AwsConfigs {

    private String dynamodbRequestDeliveryTable;
    private String dynamodbAddressTable;
    private String dynamodbTenderTable;
    private String dynamodbPaperChannelTenderTable;
    private String dynamodbPaperChannelGeoKeyTable;
    private String dynamodbPaperChannelCostTable;
    private String dynamodbDeliveryDriverTable;
    private String dynamodbCostTable;
    private String dynamodbZoneTable;
    private String dynamodbCapTable;
    private String dynamodbDeliveryFileTable;
    private String dynamodbPaperRequestErrorTable;
    private String dynamodbPaperEventsTable;
    private String dynamodbClientTable;
    private String dynamodbAttachmentsConfigTable;
    private String dynamodbPaperEventErrorTable;
    private String dynamodbPaperChannelDeliveryDriverTable;
    private String dynamodbPaperChannelAddressTable;
}
