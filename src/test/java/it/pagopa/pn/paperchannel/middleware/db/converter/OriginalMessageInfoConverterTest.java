package it.pagopa.pn.paperchannel.middleware.db.converter;

import it.pagopa.pn.paperchannel.middleware.db.entities.OriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OriginalMessageInfoConverterTest {

    private OriginalMessageInfoConverter originalMessageInfoConverter;

    @BeforeEach
    public void init() {
        this.originalMessageInfoConverter = new OriginalMessageInfoConverter();
    }

    @Test
    void transformFromPaperProgressStatusEventOriginalMessageInfoTest() {

        // Given
        PaperProgressStatusEventOriginalMessageInfo messageInfo = new PaperProgressStatusEventOriginalMessageInfo();

        messageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        messageInfo.setStatusCode(RandomStringUtils.randomAlphanumeric(10));
        messageInfo.setStatusDescription(RandomStringUtils.randomAlphanumeric(15));
        messageInfo.setRegisteredLetterCode(RandomStringUtils.randomAlphanumeric(15));
        messageInfo.setProductType(RandomStringUtils.randomAlphanumeric(3));

        messageInfo.setStatusDateTime(Instant.now());
        messageInfo.setClientRequestTimeStamp(Instant.now());

        // When
        Map<String, AttributeValue> messageInfoAttributeValueMap = this.originalMessageInfoConverter
                .transformFrom(messageInfo)
                .m();

        // Then
        assertThat(messageInfoAttributeValueMap).isNotNull();

        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_EVENT_TYPE).s()).isEqualTo(messageInfo.getEventType());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_CODE).s()).isEqualTo(messageInfo.getStatusCode());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DESCRIPTION).s()).isEqualTo(messageInfo.getStatusDescription());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_REGISTERED_LETTER_CODE).s()).isEqualTo(messageInfo.getRegisteredLetterCode());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_PRODUCT_TYPE).s()).isEqualTo(messageInfo.getProductType());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DATE_TIME).s()).isEqualTo(messageInfo.getStatusDateTime().toString());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_CLIENT_REQUEST_TIMESTAMP).s()).isEqualTo(messageInfo.getClientRequestTimeStamp().toString());
    }

    @Test
    void transformFromPaperProgressStatusEventOriginalMessageInfoWithNullFieldsTest() {

        // Given
        PaperProgressStatusEventOriginalMessageInfo messageInfo = new PaperProgressStatusEventOriginalMessageInfo();

        messageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());

        // When
        Map<String, AttributeValue> messageInfoAttributeValueMap = this.originalMessageInfoConverter
            .transformFrom(messageInfo)
            .m();

        // Then
        assertThat(messageInfoAttributeValueMap).isNotNull();

        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_EVENT_TYPE).s()).isEqualTo(messageInfo.getEventType());
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_CODE)).isNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DESCRIPTION)).isNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_REGISTERED_LETTER_CODE)).isNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_PRODUCT_TYPE)).isNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DATE_TIME)).isNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_CLIENT_REQUEST_TIMESTAMP)).isNull();
    }

    @Test
    void transformFromUnknownOriginalMessageInfoTest() {

        // Given
        OriginalMessageInfo messageInfo = new OriginalMessageInfo();
        messageInfo.setEventType(RandomStringUtils.randomAlphanumeric(10));

        // When
        Map<String, AttributeValue> messageInfoAttributeValueMap = this.originalMessageInfoConverter
                .transformFrom(messageInfo)
                .m();

        // Then
        assertThat(messageInfoAttributeValueMap).isNotNull();
        assertThat(messageInfoAttributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_EVENT_TYPE).s()).isEqualTo(messageInfo.getEventType());
    }

    @Test
    void transformToPaperProgressStatusEventOriginalMessageInfoTest() {

        // Given
        AttributeValue eventTypeAttributeValue = AttributeValue.fromS(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        AttributeValue statusCodeAttributeValue = AttributeValue.fromS(RandomStringUtils.randomAlphanumeric(10));
        AttributeValue statusDescriptionAttributeValue = AttributeValue.fromS(RandomStringUtils.randomAlphanumeric(15));
        AttributeValue registeredLetterCodeAttributeValue = AttributeValue.fromS(RandomStringUtils.randomAlphanumeric(15));
        AttributeValue productTypeAttributeValue = AttributeValue.fromS(RandomStringUtils.randomAlphanumeric(3));
        AttributeValue statusDateTimeAttributeValue = AttributeValue.fromS(Instant.now().toString());
        AttributeValue clientRequestTimeStampAttributeValue = AttributeValue.fromS(Instant.now().toString());


        AttributeValue originalMessageInfoAttributeValue = AttributeValue.fromM(Map.of(
            PaperProgressStatusEventOriginalMessageInfo.COL_EVENT_TYPE, eventTypeAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_CODE, statusCodeAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DESCRIPTION, statusDescriptionAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_REGISTERED_LETTER_CODE, registeredLetterCodeAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_PRODUCT_TYPE, productTypeAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DATE_TIME, statusDateTimeAttributeValue,
            PaperProgressStatusEventOriginalMessageInfo.COL_CLIENT_REQUEST_TIMESTAMP, clientRequestTimeStampAttributeValue
        ));

        // When
        OriginalMessageInfo originalMessageInfo = this.originalMessageInfoConverter.transformTo(originalMessageInfoAttributeValue);

        // Then
        assertThat(originalMessageInfo)
                .isNotNull()
                .isInstanceOf(PaperProgressStatusEventOriginalMessageInfo.class);

        assertThat(originalMessageInfo.getEventType()).isEqualTo(eventTypeAttributeValue.s());

        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getStatusCode()).isEqualTo(statusCodeAttributeValue.s());
        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getStatusDescription()).isEqualTo(statusDescriptionAttributeValue.s());
        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getRegisteredLetterCode()).isEqualTo(registeredLetterCodeAttributeValue.s());
        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getProductType()).isEqualTo(productTypeAttributeValue.s());
        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getStatusDateTime()).isEqualTo(statusDateTimeAttributeValue.s());
        assertThat(((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).getClientRequestTimeStamp()).isEqualTo(clientRequestTimeStampAttributeValue.s());
    }

    @Test
    void transformToUnknownOriginalMessageInfoTest() {

        // Given
        AttributeValue eventTypeAttributeValue = AttributeValue.fromS(RandomStringUtils.randomAlphanumeric(10));

        AttributeValue originalMessageInfoAttributeValue = AttributeValue.fromM(Map.of(
                PaperProgressStatusEventOriginalMessageInfo.COL_EVENT_TYPE, eventTypeAttributeValue
        ));

        // When
        OriginalMessageInfo originalMessageInfo = this.originalMessageInfoConverter.transformTo(originalMessageInfoAttributeValue);

        // Then
        assertThat(originalMessageInfo)
                .isNotNull()
                .isInstanceOf(OriginalMessageInfo.class);

        assertThat(originalMessageInfo.getEventType()).isEqualTo(eventTypeAttributeValue.s());
    }
}
