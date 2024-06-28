package it.pagopa.pn.paperchannel.utils;

/**
 * This class groups and centralizes attributes related to feedback.
 *
 * @param oldFeedbackDeliveryFailureCause There may not have been a delivery failure cause, so it can be null.
 */
public record FeedbackStatus(String oldFeedbackStatusCode, String newFeedbackStatusCode,
                             String oldFeedbackStatusDateTime, String newFeedbackStatusDateTime,
                             String oldFeedbackDeliveryFailureCause, String newFeedbackDeliveryFailureCause) {
}
