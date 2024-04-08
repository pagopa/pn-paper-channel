package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.OriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.FlowTypeEnum;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventErrorDAOTestIT extends BaseTest {

    @Autowired
    private PnEventErrorDAO pnEventErrorDAO;

    @Test
    void findEventErrorsByRequestIdTest() {

        // Given
        PaperProgressStatusEventOriginalMessageInfo originalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        originalMessageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        originalMessageInfo.setStatusCode(RandomStringUtils.randomAlphanumeric(10));
        originalMessageInfo.setStatusDescription(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setRegisteredLetterCode(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setProductType(RandomStringUtils.randomAlphanumeric(3));

        originalMessageInfo.setStatusDateTime(Instant.now());
        originalMessageInfo.setClientRequestTimeStamp(Instant.now());

        PnEventError pnEventError1 = this.buildPnEventError("request1", originalMessageInfo);
        PnEventError pnEventError2 = this.buildPnEventError("request1", originalMessageInfo);
        PnEventError pnEventError3 = this.buildPnEventError("request2", originalMessageInfo);

        this.pnEventErrorDAO.putItem(pnEventError1).block();
        this.pnEventErrorDAO.putItem(pnEventError2).block();
        this.pnEventErrorDAO.putItem(pnEventError3).block();

        // When
        List<PnEventError> eventErrors = this.pnEventErrorDAO.findEventErrorsByRequestId("request1").collectList().block();

        // Then
        assertThat(eventErrors)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(List.of(pnEventError1, pnEventError2));
    }

    @Test
    void putItemTest() {

        // Given
        String requestId = RandomStringUtils.randomAlphanumeric(10);

        PaperProgressStatusEventOriginalMessageInfo originalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        originalMessageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        originalMessageInfo.setStatusCode(RandomStringUtils.randomAlphanumeric(10));
        originalMessageInfo.setStatusDescription(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setRegisteredLetterCode(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setProductType(RandomStringUtils.randomAlphanumeric(3));

        originalMessageInfo.setStatusDateTime(Instant.now());
        originalMessageInfo.setClientRequestTimeStamp(Instant.now());

        PnEventError pnEventError = this.buildPnEventError(requestId, originalMessageInfo);

        // When
        PnEventError eventError = this.pnEventErrorDAO.putItem(pnEventError).block();

        // Then
        assertThat(eventError)
                .isNotNull()
                .isEqualTo(pnEventError);
    }

    @Test
    void deleteItemTest() {

        // Given
        String requestId = RandomStringUtils.randomAlphanumeric(10);

        PaperProgressStatusEventOriginalMessageInfo originalMessageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        originalMessageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        originalMessageInfo.setStatusCode(RandomStringUtils.randomAlphanumeric(10));
        originalMessageInfo.setStatusDescription(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setRegisteredLetterCode(RandomStringUtils.randomAlphanumeric(15));
        originalMessageInfo.setProductType(RandomStringUtils.randomAlphanumeric(3));

        originalMessageInfo.setStatusDateTime(Instant.now());
        originalMessageInfo.setClientRequestTimeStamp(Instant.now());

        PnEventError pnEventError = this.buildPnEventError(requestId, originalMessageInfo);
        this.pnEventErrorDAO.putItem(pnEventError).block();

        // When
        PnEventError deleteEventError = this.pnEventErrorDAO.deleteItem(
                pnEventError.getRequestId(),
                pnEventError.getStatusBusinessDateTime()
        ).block();

        List<PnEventError> eventErrors = this.pnEventErrorDAO
                .findEventErrorsByRequestId(requestId)
                .collectList()
                .block();

        // Then
        assertThat(deleteEventError)
                .isNotNull()
                .isEqualTo(pnEventError);

        /* Verify that after delete table is empty for the requestId PK */
        assertThat(eventErrors).isEmpty();
    }

    private PnEventError buildPnEventError(String requestId, OriginalMessageInfo originalMessageInfo) {
        Instant currentTime = Instant.now();

        PnEventError pnEventError = new PnEventError();
        pnEventError.setRequestId(requestId);
        pnEventError.setStatusBusinessDateTime(currentTime);
        pnEventError.setIun(RandomStringUtils.randomAlphanumeric(10));
        pnEventError.setStatusCode(RandomStringUtils.randomAlphanumeric(10));
        pnEventError.setOriginalMessageInfo(originalMessageInfo);
        pnEventError.setCreatedAt(currentTime);
        pnEventError.setFlowType(FlowTypeEnum.COMPLEX_890.name());
        pnEventError.setDriverCode(RandomStringUtils.randomAlphanumeric(10));
        pnEventError.setTenderCode(RandomStringUtils.randomAlphanumeric(10));

        return pnEventError;
    }
}
