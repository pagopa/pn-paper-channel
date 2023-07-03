package it.pagopa.pn.paperchannel.service.tenders;


import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.InstanceCreator;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllDeleteFromServiceTest extends BaseTest {

    @Autowired
    private PaperChannelServiceImpl paperChannelService;
    @MockBean
    private CostDAO costDAO;
    @MockBean
    private DeliveryDriverDAO deliveryDriverDAO;
    @MockBean
    private TenderDAO tenderDAO;
    private PnTender pnTenderOK;
    private PnDeliveryDriver pnDeliveryDriver;
    private PnCost pnCostNational;
    private PnCost pnCostInternational;

    @BeforeEach
    void setUp(){
        init();
    }

    @Test
    @DisplayName("whenTryDeleteTenderWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteTenderWithTenderStatusInProgress(){
        pnTenderOK.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        StepVerifier.create(this.paperChannelService.deleteTender(pnTenderOK.getTenderCode()))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                })
                .verify();
    }
    @Test
    @DisplayName("whenDeleteTenderCorrectStatusThenReturnOK")
    void deleteTenderOK(){

        Mockito.when(this.tenderDAO.deleteTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.just(pnCostNational, pnCostInternational));

        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnCost()));


        Mockito.when(this.deliveryDriverDAO.deleteDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(pnDeliveryDriver));

        StepVerifier.create(this.paperChannelService.deleteTender(pnTenderOK.getTenderCode()))
                .verifyComplete();
    }

    @Test
    @DisplayName("whenTryDeleteDriverWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteDriverWithTenderStatusInProgress(){
        pnTenderOK.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        StepVerifier.create(this.paperChannelService.deleteDriver(pnTenderOK.getTenderCode(), "12233"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DRIVER_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenTryDeleteDriverWithTenderCorrectStatusThenReturnOK")
    void deleteDriverOK(){
        pnTenderOK.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.just(pnCostNational, pnCostInternational));

        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnCost()));

        Mockito.when(this.deliveryDriverDAO.deleteDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(pnDeliveryDriver));

        StepVerifier.create(this.paperChannelService.deleteDriver(pnTenderOK.getTenderCode(), "1223"))
                .verifyComplete();
    }

    @Test
    @DisplayName("whenTryDeleteCostWithTenderStatusDifferentToCreatedThenThrowException")
    void deleteCostWithTenderStatusInProgress(){
        pnTenderOK.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        StepVerifier.create(this.paperChannelService.deleteCost(pnTenderOK.getTenderCode(), "12233", "UUID"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_CANNOT_BE_DELETED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenTryDeleteCostWithTenderCorrectStatusThenReturnOK")
    void deleteCostOK(){
        Mockito.when(this.costDAO.deleteCost(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new PnCost()));

        StepVerifier.create(this.paperChannelService.deleteCost("123", "1223", "UUID"))
                .verifyComplete();
    }


    private void init(){
        pnTenderOK = InstanceCreator.getListTender(1).get(0);
        pnTenderOK.setTenderCode("tender_code_ok");
        pnTenderOK.setStartDate(Instant.now());
        pnTenderOK.setEndDate(Instant.now().plus(3, ChronoUnit.DAYS));

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(pnTenderOK));

        pnDeliveryDriver = InstanceCreator.getDriver(true);

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.just(pnDeliveryDriver));


        pnCostNational = InstanceCreator.getCost(pnTenderOK.getTenderCode(), null, List.of("21023"), "AR");
        pnCostInternational = InstanceCreator.getCost(pnTenderOK.getTenderCode(), "ZONE_1", null, "AR");


    }

}
