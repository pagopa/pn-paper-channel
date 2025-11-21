package it.pagopa.pn.paperchannel.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnPreparePaperchannelToDelayerEvent;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.*;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@ActiveProfiles("local")
@Configuration
@Slf4j
@RequiredArgsConstructor
public class PnPaperChannelMiddlewareConfigs {


    private final PnPaperChannelConfig pnPaperChannelConfig;


    @Bean
    public DeliveryPushMomProducer deliveryMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new DeliveryPushMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueDeliveryPush(), objMapper, DeliveryPushEvent.class);
    }

    @Bean
    public InternalQueueMomProducer internalQueueMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new InternalQueueMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueInternal(), objMapper, InternalPushEvent.class);
    }

    @Bean
    public NormalizeAddressQueueMomProducer normalizeAddressQueueMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new NormalizeAddressQueueMomProducer(sqsClient, this.pnPaperChannelConfig.getQueueNormalizeAddress(), objMapper, AttemptPushEvent.class);
    }

    @Bean
    public PaperchannelToDelayerMomProducer paperchannelToDelayerMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new PaperchannelToDelayerMomProducer(sqsClient, this.pnPaperChannelConfig.getQueuePaperchannelToDelayer(), objMapper, PnPreparePaperchannelToDelayerEvent.class);
    }

    @Bean
    public DelayerToPaperchannelInternalProducer delayerToPaperchannelInternalProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new DelayerToPaperchannelInternalProducer(sqsClient, this.pnPaperChannelConfig.getQueueDelayerToPaperchannel(), objMapper, AttemptPushEvent.class);
    }

    @Bean
    public OcrProducer ocrInputsProducer(ObjectMapper objMapper) {
        SqsClient sqsClient = SqsClient.builder()
                .region(Region.of(this.pnPaperChannelConfig.getQueueRegionOcrInputs()))
                .build();
        String ocrInputQueueUrl = this.pnPaperChannelConfig.getQueueUrlOcrInputs();
        return new OcrProducer(sqsClient, ocrInputQueueUrl, ocrInputQueueUrl, objMapper, OcrInputEvent.class);
    }
}

