package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.service.NotificationReworkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {NotificationReworkV1Controller.class})
public class NotificationReworkV1ControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private NotificationReworkService notificationReworkService;

    @MockBean
    private PnClientDAO pnClientDAO;

    @Test
    void testInitNotificationReworkOk() {
        String path = "/paper-channel-private/v1/rework/{requestId}/init";
        String requestId = "requestId1";
        String reworkId = "reworkId1";

        when(notificationReworkService.initNotificationRework(requestId, reworkId))
                .thenReturn(Mono.empty());
        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("reworkId", reworkId).build(requestId))
                .exchange()
                .expectStatus()
                .isNoContent();
    }
}
