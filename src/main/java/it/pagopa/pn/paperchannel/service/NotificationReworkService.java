package it.pagopa.pn.paperchannel.service;

import reactor.core.publisher.Mono;

public interface NotificationReworkService {
    Mono<Void> initNotificationRework(String requestId, String reworkId);
}
