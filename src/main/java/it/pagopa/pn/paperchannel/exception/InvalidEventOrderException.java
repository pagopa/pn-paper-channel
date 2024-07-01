package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.FeedbackStatus;
import lombok.Getter;

/**
 * Exception thrown when events are received in an invalid order.
 * This exception helps identify the type of error using feedbackStatus object.
 */
@Getter
public class InvalidEventOrderException extends RuntimeException {
    private final ExceptionTypeEnum exceptionType;
    private final String message;
    private final FeedbackStatus feedbackStatus;

    /**
     * Constructs a new InvalidEventOrderException with the specified details.
     *
     * @param exceptionType   the type of the exception
     * @param message         a descriptive message for the exception
     * @param feedbackStatus  the feedback status associated with this exception
     */
    public InvalidEventOrderException(ExceptionTypeEnum exceptionType, String message, FeedbackStatus feedbackStatus){
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
        this.feedbackStatus = feedbackStatus;
    }

    /**
     * Creates an instance of InvalidEventOrderException using the provided delivery request and
     * paper request.
     *
     * @param pnDeliveryRequest The PnDeliveryRequest containing the previous feedback details.
     * @param paperRequest      The PaperProgressStatusEventDto containing the new feedback details.
     * @param message           A custom message that can include contextual information such as the requestId.
     * @return                  An instance of InvalidEventOrderException fully populated with error details.
     */
    public static InvalidEventOrderException from(PnDeliveryRequest pnDeliveryRequest,
                       PaperProgressStatusEventDto paperRequest,
                       String message) {
        return new InvalidEventOrderException(
                ExceptionTypeEnum.WRONG_EVENT_ORDER,
                message,
                new FeedbackStatus(
                        pnDeliveryRequest.getFeedbackStatusCode(),
                        paperRequest.getStatusCode(),
                        pnDeliveryRequest.getFeedbackStatusDateTime(),
                        paperRequest.getStatusDateTime().toInstant(),
                        pnDeliveryRequest.getFeedbackDeliveryFailureCause(),
                        paperRequest.getDeliveryFailureCause()
                )
        );
    }
}