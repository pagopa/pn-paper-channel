package it.pagopa.pn.paperchannel.model;


public enum ProductTypeEnum {
    RN_AR("RN_AR"),

    RN_890("RN_890"),

    RN_RS("RN_RS");

    private final String value;

    ProductTypeEnum(String value) {
        this.value = value;
    }

    public static ProductTypeEnum fromValue(String value) {
        for (ProductTypeEnum b : ProductTypeEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
