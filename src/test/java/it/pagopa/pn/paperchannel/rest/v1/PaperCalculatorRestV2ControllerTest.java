package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;


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
        //ARRANGE
        String tenderId = "GARA_2024";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void testCostSimulator400ThrowExceptionWhenParametersNotFound() {
        //ARRANGE
        String tenderId = "GARA";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testCostSimulator404ThrowExceptionWhenApiIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        ShipmentCalculateResponse shipmentCalculateResponse = new ShipmentCalculateResponse();
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculates");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenReturn(Mono.just(shipmentCalculateResponse));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void testCostSimulator404ThrowExceptionWhenTenderIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenThrow(new PnGenericException(TENDER_NOT_EXISTED, TENDER_NOT_EXISTED.getMessage(), HttpStatus.NOT_FOUND));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo(TENDER_NOT_EXISTED.getTitle())
                .jsonPath("$.detail").isEqualTo(TENDER_NOT_EXISTED.getMessage());
    }

    @Test
    void testCostSimulator404ThrowExceptionWhenGeokeyIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenThrow(new PnGenericException(GEOKEY_NOT_FOUND, GEOKEY_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo(GEOKEY_NOT_FOUND.getTitle())
                .jsonPath("$.detail").isEqualTo(GEOKEY_NOT_FOUND.getMessage());
    }

    @Test
    void testCostSimulator404ThrowExceptionWhenCostIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String path = "/paper-channel-private/v2/tenders/".concat(tenderId).concat("/cost/calculate");
        Mockito.when(paperCalculatorUtils.costSimulator(tenderId, getShipmentCalculateRequest()))
                .thenThrow(new PnGenericException(COST_DRIVER_OR_FSU_NOT_FOUND, COST_DRIVER_OR_FSU_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        //ACT & ASSERT
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getShipmentCalculateRequest())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo(COST_DRIVER_OR_FSU_NOT_FOUND.getTitle())
                .jsonPath("$.detail").isEqualTo(COST_DRIVER_OR_FSU_NOT_FOUND.getMessage());
    }

    private ShipmentCalculateRequest getShipmentCalculateRequest(){
        ShipmentCalculateRequest request = new ShipmentCalculateRequest();
        request.setGeokey("GARA_2024");
        request.setProduct(ShipmentCalculateRequest.ProductEnum.RS);
        request.setNumSides(5);
        request.setIsReversePrinter(true);
        request.setPageWeight(10);
        return request;
    }
}