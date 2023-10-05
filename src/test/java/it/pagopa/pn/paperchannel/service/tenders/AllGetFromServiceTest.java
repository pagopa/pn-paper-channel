package it.pagopa.pn.paperchannel.service.tenders;

import it.pagopa.pn.paperchannel.config.InstanceCreator;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_DRIVER_NOT_EXISTED;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.TENDER_NOT_EXISTED;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AllGetFromServiceTest {
    @InjectMocks
    private PaperChannelServiceImpl paperChannelService;
    @Mock
    private CostDAO costDAO;
    @Mock
    private DeliveryDriverDAO deliveryDriverDAO;
    @Mock
    private TenderDAO tenderDAO;

    @Test
    @DisplayName("whenRetrieveAllTendersFromPageOne")
    void getAllTenderWithElementInPageTest(){
        Mockito.when(this.tenderDAO.getTenders())
                .thenReturn(Mono.just(InstanceCreator.getListTender(5)));

        PageableTenderResponseDto response = this.paperChannelService.getAllTender(1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(5, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllTendersFromPageOneWithMorePage")
    void getAllTenderWithElementInPageWithMorePageTest(){
        Mockito.when(this.tenderDAO.getTenders())
                .thenReturn(Mono.just(InstanceCreator.getListTender(25)));

        PageableTenderResponseDto response = this.paperChannelService.getAllTender(1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(25, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveDetailTenderThenReturnResponse")
    void getDetailTenderFromCode(){
        Mockito.when(tenderDAO.getTender("1234"))
                .thenReturn(Mono.just(InstanceCreator.getListTender(1).get(0)));

        TenderDetailResponseDTO response = this.paperChannelService.getTenderDetails("1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(TenderDetailResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals("Tender_0", response.getTender().getCode());
    }

    @Test
    @DisplayName("whenRetrieveDetailTenderNotExistThenThrowError")
    void getDetailTenderFromCodeThrowError(){
        Mockito.when(tenderDAO.getTender("1234"))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.getTenderDetails("1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveDetailDriverThenReturnResponse")
    void getDetailDriverFromCode(){
        PnDeliveryDriver driver = InstanceCreator.getDriver(true);

        Mockito.when(deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(driver));

        DeliveryDriverResponseDTO response = this.paperChannelService.getDriverDetails("1234", "1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(DeliveryDriverResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals(driver.getTaxId(), response.getDriver().getTaxId());
    }

    @Test
    @DisplayName("whenRetrieveDetailDriverNotExistThenThrowError")
    void getDetailDriverFromCodeThatNotExistThrowError(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.getDriverDetails("1234", "1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveDetailFSUThenReturnResponse")
    void getDetailFSUFromCode(){
        PnDeliveryDriver driver = InstanceCreator.getDriver(true);

        Mockito.when(deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(driver));

        FSUResponseDTO response = this.paperChannelService.getDetailsFSU("1234").block();
        assertNotNull(response);
        assertEquals(true, response.getResult());
        assertEquals(FSUResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertEquals(driver.getTaxId(), response.getFsu().getTaxId());
    }

    @Test
    @DisplayName("whenRetrieveDetailFSUNotExistThenThrowError")
    void getDetailFSUFromCodeThatNotExistThrowError(){
        Mockito.when(deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.getDetailsFSU("1234"))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());return true;
                }).verify();
    }

    @Test
    @DisplayName("whenRetrieveAllDriversFromPageOne")
    void getAllDriversWithElementInPageTest(){
        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(InstanceCreator.getListDriver(5).stream()));

        PageableDeliveryDriverResponseDto response =
                this.paperChannelService.getAllDeliveriesDrivers("1234", 1, 10, true).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(5, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllDriversFromPageOneWithMorePage")
    void getAllDriversWithElementInPageWithMorePageTest(){
        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFromTender(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(InstanceCreator.getListDriver(25).stream()));

        PageableDeliveryDriverResponseDto response =
                this.paperChannelService.getAllDeliveriesDrivers("1234", 1, 10, true).block();

        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(25, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllCostsFromPageOne")
    void getAllCostsWithElementInPageTest(){
        List<PnCost> moreCost = new ArrayList<>();
        moreCost.addAll(InstanceCreator.getAllNationalCost("1234","1234", false));
        moreCost.addAll(InstanceCreator.getAllInternationalCost("1234","1234", false));
        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(moreCost.stream()));

        PageableCostResponseDto response =
                this.paperChannelService.getAllCostFromTenderAndDriver("1234","1234", 1, 10).block();
        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(true, response.getLast());
        assertEquals(9, response.getNumberOfElements());
        assertEquals(0, response.getNumber());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    @DisplayName("whenRetrieveAllCostsFromPageOneWithMorePage")
    void getAllCostsWithElementInPageWithMorePageTest(){
        List<PnCost> moreCost = new ArrayList<>();
        moreCost.addAll(InstanceCreator.getAllNationalCost("1234","1234", false));
        moreCost.addAll(InstanceCreator.getAllInternationalCost("1234","1234", false));

        moreCost.addAll(InstanceCreator.getAllNationalCost("1234","1235", true));
        moreCost.addAll(InstanceCreator.getAllInternationalCost("1234","1235", true));

        moreCost.addAll(InstanceCreator.getAllNationalCost("1234","1236", false));
        moreCost.addAll(InstanceCreator.getAllInternationalCost("1234","1236", false));

        moreCost.addAll(InstanceCreator.getAllNationalCost("1234","1237", false));
        moreCost.addAll(InstanceCreator.getAllInternationalCost("1234","1237", false));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(moreCost.stream()));

        PageableCostResponseDto response =
                this.paperChannelService.getAllCostFromTenderAndDriver("1234","1234", 1, 10).block();

        assertNotNull(response);
        assertEquals(true, response.getFirst());
        assertEquals(false, response.getLast());
        assertEquals(10, response.getNumberOfElements());
        assertEquals(36, response.getTotalElements());
        assertEquals(0, response.getNumber());
        assertEquals(4, response.getTotalPages());
    }
}
