package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;



@WebFluxTest(controllers = {PaperCalculatorRestV2Controller.class})
public class PaperCalculatorRestV2ControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private PaperCalculatorUtils paperCalculatorUtils;
    @MockBean
    private PnClientDAO pnClientDAO;


    @Test
    void testCostSimulator() {
        String tenderId = "GARA_2024";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void testCostSimulator400ErrBadRequest() {
        String tenderId = "GARA";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testCostSimulator404ErrNotFound() {
        String tenderId = "GARA_2024";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculates");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private ShipmentCalculateRequest getShipmentCalculateRequest(){
        ShipmentCalculateRequest request = new ShipmentCalculateRequest();
        request.setGeokey("GARA_2024");
        request.setProduct(ShipmentCalculateRequest.ProductEnum.RS);
        request.setNumPages(5);
        request.setIsReversePrinter(true);
        request.setPageWeight(10);
        return request;
    }
}