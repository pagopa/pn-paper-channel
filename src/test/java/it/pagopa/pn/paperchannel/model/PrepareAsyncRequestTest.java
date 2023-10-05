package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class PrepareAsyncRequestTest {
    private String requestId;
    private String iun;
    private String correlationId;
    private Address address;
    private boolean isAddressRetry = false;
    private Integer attemptRetry;
    private String clientId;
    private boolean isF24flow = false;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest(requestId, iun, correlationId, address, false, attemptRetry);
        prepareAsyncRequest.setClientId(clientId);
        Assertions.assertNotNull(prepareAsyncRequest);
    }

    @Test
    void toStringTest() {
        PrepareAsyncRequest prepareAsyncRequest = initPrepareAsyncRequest();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prepareAsyncRequest.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("requestId=");
        stringBuilder.append(requestId);
        stringBuilder.append(", ");
        stringBuilder.append("iun=");
        stringBuilder.append(iun);
        stringBuilder.append(", ");
        stringBuilder.append("correlationId=");
        stringBuilder.append(correlationId);
        stringBuilder.append(", ");
        stringBuilder.append("address=");
        stringBuilder.append(address);
        stringBuilder.append(", ");
        stringBuilder.append("isAddressRetry=");
        stringBuilder.append(isAddressRetry);
        stringBuilder.append(", ");
        stringBuilder.append("isF24ResponseFlow=");
        stringBuilder.append(isF24flow);
        stringBuilder.append(", ");
        stringBuilder.append("attemptRetry=");
        stringBuilder.append(attemptRetry);
        stringBuilder.append(", ");
        stringBuilder.append("clientId=");
        stringBuilder.append(clientId);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        log.info(toTest);
        log.info(prepareAsyncRequest.toString());
        Assertions.assertEquals(toTest, prepareAsyncRequest.toString());
    }

    private PrepareAsyncRequest initPrepareAsyncRequest() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        prepareAsyncRequest.setRequestId(requestId);
        prepareAsyncRequest.setIun(iun);
        prepareAsyncRequest.setCorrelationId(correlationId);
        prepareAsyncRequest.setAddress(address);
        prepareAsyncRequest.setAttemptRetry(attemptRetry);
        prepareAsyncRequest.setClientId(clientId);
        return prepareAsyncRequest;
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        iun = "ABCD-HILM-YKWX-202202-1";
        correlationId = "ZAXSCDVFBGNH";
        address = new Address();
        address.setAddress("Via della fiera");
        address.setPr("pr");
        address.setCountry("Italy");
        address.setAddressRow2("Via della mosca");
        address.setCity("Roma");
        address.setCity2("Milano");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("nameRow2");
        address.setFromNationalRegistry(false);
        address.setFlowType(Const.PREPARE);
        address.setCap("00100");
        address.setProductType("890");
        isAddressRetry = false;
        attemptRetry = 3;
        clientId = "ABC";
        isF24flow = false;
    }
}
