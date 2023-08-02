package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.ZoneDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import it.pagopa.pn.paperchannel.service.impl.PaperTenderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ACTIVE_TENDER_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_DRIVER_OR_FSU_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaperTenderServiceTest {

    @InjectMocks
    private PaperTenderServiceImpl paperTenderService;

    @Mock
    private ZoneDAO zoneDAO;
    @Mock
    private CostDAO costDAO;
    @Mock
    private TenderDAO tenderDAO;
    private PnTender tender;
    private PnCost cost;


    @Test
    void whenCallGetCostWithoutActiveTenderThenReturnErrorTest(){
        Mockito.when(tenderDAO.findActiveTender()).thenReturn(Mono.empty());
        StepVerifier.create(this.paperTenderService.getCostFrom(null, "ZONE_1", "AR"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(ACTIVE_TENDER_NOT_FOUND, ((PnGenericException) ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    void whenCallGetCostFromTenderAndCapNotExistReturnErrorTest(){
        mockTender();
        Mockito.when(costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(), "89321", null, "AR"))
                        .thenReturn(Mono.empty());
        StepVerifier.create(this.paperTenderService.getCostFrom("89321", null, "AR"))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND, ((PnGenericException) ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    void whenCallGetCostFromTenderAndZoneReturnCostDTOTest(){
        mockTender();
        mockCost();
        CostDTO costDTO = this.paperTenderService.getCostFrom(null, cost.getZone(), cost.getProductType()).block();
        assertNotNull(costDTO);
        assertEquals(cost.getUuid(), costDTO.getUid());
        assertEquals(cost.getCap(), costDTO.getCap());
        assertEquals(cost.getBasePrice(), costDTO.getPrice());
        assertEquals(cost.getPagePrice(), costDTO.getPriceAdditional());
        assertEquals(cost.getProductType(), costDTO.getProductType().getValue());
    }

    @Test
    void getZoneFromCountryTest(){
        mockZone();
        String zone = this.paperTenderService.getZoneFromCountry("Germany").block();
        assertNotNull(zone);
        assertEquals("ZONE_1", zone);
    }

    private void mockZone() {
        PnZone zone = new PnZone();
        zone.setZone("ZONE_1");
        zone.setCountryEn("Germany");
        zone.setCountryIt("Germania");
        Mockito.when(zoneDAO.getByCountry("Germany")).thenReturn(Mono.just(zone));
    }

    private void mockTender() {
        tender = new PnTender();
        tender.setStatus("IN_PROGRESS");
        tender.setTenderCode("CODE_TENDER");
        Mockito.when(tenderDAO.findActiveTender()).thenReturn(Mono.just(tender));
    }

    private void mockCost(){

        cost = new PnCost();
        cost.setFsu(false);
        cost.setCap(null);
        cost.setProductType("AR");
        cost.setPagePrice(1.23F);
        cost.setUuid("COST_UUID");
        cost.setZone("ZONE_1");
        cost.setBasePrice(1.43F);
        cost.setDeliveryDriverCode("DRIVER");
        cost.setTenderCode(tender.getTenderCode());
        Mockito.when(costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(),  null, cost.getZone(), cost.getProductType()))
                .thenReturn(Mono.just(cost));
    }

}
