package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

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

    @Test
    void createMETAForPNAG012EventTest() {
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusDateTime(OffsetDateTime.now());

        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setStatusDateTime(Instant.now());

        PnEventMeta metaPNAG012 = MetaDematUtils.createMETAForPNAG012Event(paperRequest,pnEventMeta, 365L);
        assertThat(metaPNAG012)
                .isNotNull()
                .hasFieldOrPropertyWithValue("statusCode", MetaDematUtils.PNAG012_STATUS_CODE)
                .hasFieldOrPropertyWithValue("statusDateTime", pnEventMeta.getStatusDateTime());
    }

}
