package it.pagopa.pn.paperchannel.utils;

import lombok.NonNull;

/**
 * This class groups and centralizes attributes related to feedback.
 *
 * @param oldFeedbackDeliveryFailureCause There may not have been a delivery failure cause, so it can be null.
 */
public record FeedbackStatus(@NonNull String oldFeedbackStatusCode, @NonNull String newFeedbackStatusCode,
                             @NonNull String oldFeedbackStatusDateTime, @NonNull String newFeedbackStatusDateTime,
                             String oldFeedbackDeliveryFailureCause, String newFeedbackDeliveryFailureCause) {
}
