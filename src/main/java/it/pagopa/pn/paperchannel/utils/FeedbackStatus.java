package it.pagopa.pn.paperchannel.utils;

import java.io.Serializable;

/**
 * This class groups and centralizes attributes related to feedback.
 */
public record FeedbackStatus(String oldFeedbackStatusCode,
                             String newFeedbackStatusCode,
                             String oldFeedbackStatusDateTime,
                             String newFeedbackStatusDateTime,
                             String oldFeedbackDeliveryFailureCause,
                             String newFeedbackDeliveryFailureCause)
                            implements Serializable {}