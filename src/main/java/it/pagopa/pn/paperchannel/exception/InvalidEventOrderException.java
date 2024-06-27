package it.pagopa.pn.paperchannel.exception;

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

}