package it.pagopa.pn.paperchannel.middleware.db.converter;

import it.pagopa.pn.paperchannel.middleware.db.entities.OriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

/**
 * DynamoDB converter for attributes of type {@link OriginalMessageInfo} that manages
 * marshalling and unmarshalling operations to and from DynamoDB table.
 * */
public class OriginalMessageInfoConverter implements AttributeConverter<OriginalMessageInfo> {

    /**
     * Transform a Java object to a DynamoDB notation object
     *
     * @param pnEventOriginalMessage    Java object to convert
     *
     * @return                          DynamoDB notation object
     * */
    @Override
    public AttributeValue transformFrom(OriginalMessageInfo pnEventOriginalMessage) {
        AttributeValue attributeValue = null;

        if (pnEventOriginalMessage.getEventType().equals(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name())) {
            attributeValue = ((PaperProgressStatusEventOriginalMessageInfo) pnEventOriginalMessage).getAttributeValue();
        }

        return attributeValue;
    }

    /**
     * Transform a DynamoDB notation object to a Java object
     *
     * @param attributeValue    DynamoDB notation object
     *
     * @return                  Java object
     * */
    @SuppressWarnings("CastCanBeRemovedNarrowingVariableType")
    @Override
    public OriginalMessageInfo transformTo(AttributeValue attributeValue) {
        OriginalMessageInfo originalMessageInfo = null;

        Map<String, AttributeValue> attributeValueMap = attributeValue.m();

        if (attributeValueMap.get(OriginalMessageInfo.COL_EVENT_TYPE).s().equals(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name())) {
            originalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
            originalMessageInfo.setEventType(attributeValueMap.get(OriginalMessageInfo.COL_EVENT_TYPE).s());
            ((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).setStatusCode(attributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_CODE).s());
            ((PaperProgressStatusEventOriginalMessageInfo) originalMessageInfo).setStatusDescription(attributeValueMap.get(PaperProgressStatusEventOriginalMessageInfo.COL_STATUS_DESCRIPTION).s());
        }

        return originalMessageInfo;
    }

    @Override
    public EnhancedType<OriginalMessageInfo> type() {
        return EnhancedType.of(OriginalMessageInfo.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}
