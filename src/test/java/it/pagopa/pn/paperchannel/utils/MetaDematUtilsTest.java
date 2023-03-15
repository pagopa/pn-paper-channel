package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaDematUtilsTest {


    @Test
    void buildDematRequestIdTest() {
        String requestId = "requestId";
        String dematRequestId = MetaDematUtils.buildDematRequestId(requestId);
        assertThat(dematRequestId).isEqualTo("DEMAT##" + requestId);
    }

    @Test
    void buildDocumentTypeStatusCodeTest() {
        String documentType = "AR";
        String statusCode = "statusCode";
        String documentTypeStatusCode = MetaDematUtils.buildDocumentTypeStatusCode(documentType, statusCode);
        assertThat(documentTypeStatusCode).isEqualTo(documentType + "##" + statusCode);
    }

    @Test
    void buildMetaRequestIdTest() {
        String requestId = "requestId";
        String dematRequestId = MetaDematUtils.buildMetaRequestId(requestId);
        assertThat(dematRequestId).isEqualTo("META##" + requestId);
    }

    @Test
    void buildMetaStatusCodeTest() {
        String statusCode = "statusCode";
        String metaStatusCode = MetaDematUtils.buildMetaStatusCode(statusCode);
        assertThat(metaStatusCode).isEqualTo("META##" + statusCode);
    }

}
