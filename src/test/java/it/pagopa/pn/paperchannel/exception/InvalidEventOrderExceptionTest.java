package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InvalidEventOrderExceptionTest {
    @Test
    public void testFrom_createsExceptionWithCorrectDetails() {
        // Arrange
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setFeedbackStatusCode("RECRN001C");
        pnDeliveryRequest.setFeedbackStatusDateTime(Instant.now().minusSeconds(3600));
        pnDeliveryRequest.setFeedbackDeliveryFailureCause("Delivery Failure Cause");

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId(pnDeliveryRequest.getRequestId());
        paperRequest.setStatusCode("RECRN002C");
        paperRequest.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        paperRequest.setDeliveryFailureCause("New Delivery Failure Cause");

        String expectedMessage = "Feedback event order is incorrect for requestId: " + paperRequest.getRequestId();

        // Act
        InvalidEventOrderException exception = InvalidEventOrderException.from(pnDeliveryRequest, paperRequest, expectedMessage);

        // Assert
        assertEquals(ExceptionTypeEnum.WRONG_EVENT_ORDER, exception.getExceptionType());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(pnDeliveryRequest.getFeedbackStatusCode(), exception.getFeedbackStatus().oldFeedbackStatusCode());
        assertEquals(paperRequest.getStatusCode(), exception.getFeedbackStatus().newFeedbackStatusCode());
        assertEquals(pnDeliveryRequest.getFeedbackStatusDateTime(), exception.getFeedbackStatus().oldFeedbackStatusDateTime());
        assertEquals(paperRequest.getStatusDateTime().toInstant(), exception.getFeedbackStatus().newFeedbackStatusDateTime());
        assertEquals(pnDeliveryRequest.getFeedbackDeliveryFailureCause(), exception.getFeedbackStatus().oldFeedbackDeliveryFailureCause());
        assertEquals(paperRequest.getDeliveryFailureCause(), exception.getFeedbackStatus().newFeedbackDeliveryFailureCause());
    }
}
