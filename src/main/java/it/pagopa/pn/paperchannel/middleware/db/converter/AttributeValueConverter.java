package it.pagopa.pn.paperchannel.middleware.db.converter;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.annotation.Nullable;
import java.util.Map;

public final class AttributeValueConverter {

    private AttributeValueConverter() {}

    /**
     * Extract string value from {@link AttributeValue} map in null pointer safe manner
     *
     * @param attributeValueMap map from which exctact value
     * @param key               map access key
     *
     * @return                  the string value
     * */
    @Nullable
    public static String getAttributeValueFromMap(Map<String, AttributeValue> attributeValueMap, String key) {
        AttributeValue attributeValue = attributeValueMap.get(key);
        return attributeValue != null ? attributeValue.s() : null;
    }

    /**
     * Insert a new key, value tuple in map if value is not null
     * @param attributeValueMap map from which exctact value
     * @param key               map access key
     * @param value             value to insert
     *
     * */
    public static void addAttributeValueToMap(Map<String, AttributeValue> attributeValueMap, String key, String value) {
        if (value != null) {
            attributeValueMap.put(key, AttributeValue.fromS(value));
        }
    }
}
