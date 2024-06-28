package it.pagopa.pn.paperchannel.utils;

import java.io.Serializable;
import java.time.Instant;

/**
 * This class groups and centralizes attributes related to feedback.
 */
public record FeedbackStatus(String oldFeedbackStatusCode,
                             String newFeedbackStatusCode,
                             Instant oldFeedbackStatusDateTime,
                             Instant newFeedbackStatusDateTime,
                             String oldFeedbackDeliveryFailureCause,
                             String newFeedbackDeliveryFailureCause)
                            implements Serializable {}