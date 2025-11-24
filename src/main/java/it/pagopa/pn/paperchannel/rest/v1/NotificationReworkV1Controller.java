package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.api.NotificationReworkApi;
import it.pagopa.pn.paperchannel.service.NotificationReworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationReworkV1Controller implements NotificationReworkApi {

    private final NotificationReworkService notificationReworkService;

    @Override
    public Mono<ResponseEntity<Void>> initNotificationRework(String requestId, String reworkId, final ServerWebExchange exchange) {
        return notificationReworkService.initNotificationRework(requestId, reworkId)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }
}
