package it.pagopa.pn.paperchannel.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalQueueMomProducerTest {

    InternalQueueMomProducer internalQueueMomProducer;

    @BeforeEach
    public void init() {
        SqsClient sqsClient = mock(SqsClient.class);
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(GetQueueUrlResponse.builder().queueUrl("url").build());
        internalQueueMomProducer = new InternalQueueMomProducer(sqsClient, "topic", new ObjectMapper(), InternalPushEvent.class);

    }


    @Test
    void getSqSHeaderAttemptEventHeaderTest() {

        Instant expired = Instant.now();
        InternalEventHeader header = InternalEventHeader.builder()
                .eventId("eventId")
                .attempt(1)
                .expired(expired)
                .eventType("eventType")
                .publisher("publisher")
                .createdAt(Instant.now())
                .build();

        var headersMap = internalQueueMomProducer.getSqSHeader(header);

        assertThat(headersMap).containsEntry("attempt", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("1")
                .build()
                )
                .containsEntry("expired", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(expired.toString())
                        .build());
    }

    @Test
    void getSqSHeaderTest() {

        GenericEventHeader header = GenericEventHeader.builder()
                .eventId("eventId")
                .eventType("eventType")
                .publisher("publisher")
                .createdAt(Instant.now())
                .build();

        var headersMap = internalQueueMomProducer.getSqSHeader(header);

        assertThat(headersMap).doesNotContainKey("attempt").doesNotContainKey("expired");
    }
}
