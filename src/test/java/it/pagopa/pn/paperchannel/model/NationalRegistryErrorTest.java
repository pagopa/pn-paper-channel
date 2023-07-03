package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NationalRegistryErrorTest {
    private String message;
    private String requestId;
    private String relatedRequestId;
    private String fiscalCode;
    private String receiverType;
    private String iun;
    private String correlationId;

    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void setGetTest() {
        NationalRegistryError nationalRegistryError = initNationalRegistryError();
        Assertions.assertEquals(message, nationalRegistryError.getMessage());
        Assertions.assertEquals(relatedRequestId, nationalRegistryError.getRelatedRequestId());
        relatedRequestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_0";
        nationalRegistryError.setRelatedRequestId(relatedRequestId);
        Assertions.assertEquals(relatedRequestId, nationalRegistryError.getRelatedRequestId());

        Assertions.assertEquals(fiscalCode, nationalRegistryError.getFiscalCode());
        Assertions.assertEquals(receiverType, nationalRegistryError.getReceiverType());
        Assertions.assertEquals(correlationId, nationalRegistryError.getCorrelationId());
    }

    private NationalRegistryError initNationalRegistryError() {
        NationalRegistryError nationalRegistryError = new NationalRegistryError();
        nationalRegistryError.setMessage(message);
        nationalRegistryError.setRequestId(requestId);
        nationalRegistryError.setRelatedRequestId(relatedRequestId);
        nationalRegistryError.setFiscalCode(fiscalCode);
        nationalRegistryError.setReceiverType(receiverType);
        nationalRegistryError.setIun(iun);
        nationalRegistryError.setCorrelationId(correlationId);
        return nationalRegistryError;
    }

    private void initialize() {
        message = "message";
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        relatedRequestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_2";
        fiscalCode = "ABCDEF98G76H543I";
        receiverType = "PF";
        iun = "ABCD-HILM-YKWX-202202-1";
        correlationId = "ZAXSCDVFBGNH";
    }
}
