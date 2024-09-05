package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.service.impl.PaperTenderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.Instant;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


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
    @Mock
    private PnPaperTenderDAO pnPaperTenderDAO;
    @Mock
    private PnPaperGeoKeyDAO pnPaperGeoKeyDAO;
    @Mock
    private PnPaperCostDAO pnPaperCostDAO;
    private PnTender tender;
    private PnCost cost;


    @Test
    void whenCallGetCostWithoutActiveTenderThenReturnErrorTest(){
        when(tenderDAO.findActiveTender()).thenReturn(Mono.empty());
        StepVerifier.create(this.paperTenderService.getCostFrom(null, "ZONE_1", "AR"))
                .expectErrorMatches((ex) -> {
                    assertInstanceOf(PnGenericException.class, ex);
                    assertEquals(ACTIVE_TENDER_NOT_FOUND, ((PnGenericException) ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    void whenCallGetCostFromTenderAndCapNotExistReturnErrorTest(){
        mockTender();
        when(costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(), "89321", null, "AR"))
                        .thenReturn(Mono.empty());
        StepVerifier.create(this.paperTenderService.getCostFrom("89321", null, "AR"))
                .expectErrorMatches((ex) -> {
                    assertInstanceOf(PnGenericException.class, ex);
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

    @Test
    void getSimplifiedCost_shouldReturnPnPaperChannelCostDTO_whenValidInputsAreProvided() {
        String cap = "000001";
        // Arrange
        PnPaperChannelTender mockTender = mockPnPaperTender();
        PnPaperChannelGeoKey mockGeoKey = mockPnPaperGeokey();
        PnPaperChannelCost mockPaperCost = mockPnPaperCost();

        PnPaperChannelCostDTO expectedDTO = new PnPaperChannelCostDTO();
        expectedDTO.setTenderId(mockTender.getTenderId());
        expectedDTO.setZone(mockGeoKey.getZone());
        expectedDTO.setLot(mockGeoKey.getLot());
        expectedDTO.setProduct(mockPaperCost.getProduct());

        when(pnPaperTenderDAO.getActiveTender()).thenReturn(Mono.just(mockTender));
        when(pnPaperGeoKeyDAO.getGeoKey(mockTender.getTenderId(), mockGeoKey.getProduct(), cap))
                .thenReturn(Mono.just(mockGeoKey));
        when(pnPaperCostDAO.getCostByTenderIdProductLotZone(mockGeoKey.getTenderId(), mockGeoKey.getProduct(), mockGeoKey.getLot(), mockGeoKey.getZone()))
                .thenReturn(Mono.just(mockPaperCost));

        // Act
        Mono<PnPaperChannelCostDTO> result = paperTenderService.getSimplifiedCost(cap, mockGeoKey.getZone(), mockGeoKey.getProduct());

        // Assert
        assertNotNull(result);
        result.subscribe(dto -> {
            assertNotNull(dto);
            assertEquals(expectedDTO.getTenderId(), dto.getTenderId());
            assertEquals(expectedDTO.getZone(), dto.getZone());
            assertEquals(expectedDTO.getLot(), dto.getLot());
            assertEquals(expectedDTO.getProduct(), dto.getProduct());
        });

        verify(pnPaperTenderDAO, times(1)).getActiveTender();
        verify(pnPaperGeoKeyDAO, times(1)).getGeoKey(mockTender.getTenderId(), mockGeoKey.getProduct(), cap);
        verify(pnPaperCostDAO, times(1)).getCostByTenderIdProductLotZone(mockGeoKey.getTenderId(), mockGeoKey.getProduct(), mockGeoKey.getLot(), mockGeoKey.getZone());
   }

    @Test
    void getSimplifiedCost_shouldThrowException_whenNoActiveTenderFound() {
        // Arrange
        String cap = "12345";
        String zone =
                "Francia";
        String productType = "AR";

        when(pnPaperTenderDAO.getActiveTender()).thenReturn(Mono.empty());

        // Act
        Mono<PnPaperChannelCostDTO> result = paperTenderService.getSimplifiedCost(cap, zone, productType);

        //Assert
        assertThrows(PnGenericException.class, result::block);

        verify(pnPaperTenderDAO, times(1)).getActiveTender();
        verify(pnPaperGeoKeyDAO, never()).getGeoKey(anyString(), anyString(), anyString());
        verify(pnPaperCostDAO, never()).getCostByTenderIdProductLotZone(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void getCostFromTenderIdAndReturnPnPaperChannelCostDTOTest() {
        // ARRANGE
        String tenderId = "GARA_2024";
        PnPaperChannelTender pnPaperChannelTender = mockPnPaperTender(tenderId);
        PnPaperChannelGeoKey pnPaperChannelGeoKey = mockPnPaperGeokey(tenderId);
        String product = pnPaperChannelGeoKey.getProduct();
        String geokey = pnPaperChannelGeoKey.getGeokey();
        String lot = pnPaperChannelGeoKey.getLot();
        String zone = pnPaperChannelGeoKey.getZone();

        PnPaperChannelCost pnPaperChannelCost = mockPnPaperCost(tenderId);
        Mockito.when(pnPaperTenderDAO.getTenderById(tenderId))
                .thenReturn(Mono.just(pnPaperChannelTender));
        Mockito.when(pnPaperGeoKeyDAO.getGeoKey(tenderId, product, geokey))
                .thenReturn(Mono.just(pnPaperChannelGeoKey));
        Mockito.when(pnPaperCostDAO.getCostByTenderIdProductLotZone(tenderId, product, lot, zone))
                .thenReturn(Mono.just(pnPaperChannelCost));

        PnPaperChannelCostDTO expectedDTO = new PnPaperChannelCostDTO();
        expectedDTO.setTenderId(pnPaperChannelTender.getTenderId());
        expectedDTO.setProduct(pnPaperChannelCost.getProduct());
        expectedDTO.setLot(pnPaperChannelGeoKey.getLot());
        expectedDTO.setZone(pnPaperChannelGeoKey.getZone());

        // ACT
        PnPaperChannelCostDTO pnPaperChannelCostDTO = paperTenderService.getCostFromTenderId(tenderId, geokey, product).block();

        //ASSERT
        assertNotNull(pnPaperChannelCostDTO);
        assertEquals(expectedDTO.getTenderId(), pnPaperChannelCostDTO.getTenderId());
        assertEquals(expectedDTO.getProduct(), pnPaperChannelCostDTO.getProduct());
        assertEquals(expectedDTO.getLot(), pnPaperChannelCostDTO.getLot());
        assertEquals(expectedDTO.getZone(), pnPaperChannelCostDTO.getZone());

        Mockito.verify(pnPaperTenderDAO, times(1)).getTenderById(tenderId);
        Mockito.verify(pnPaperGeoKeyDAO, times(1)).getGeoKey(tenderId, product, geokey);
        Mockito.verify(pnPaperCostDAO, times(1)).getCostByTenderIdProductLotZone(tenderId, product, lot, zone);
    }

    @Test
    void getCostFromTenderIdThrowExceptionWhenTenderIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String product = "RS";
        String geokey = "00100";
        String lot = "1";
        String zone = "ZONE_1";
        Mockito.when(pnPaperTenderDAO.getTenderById(tenderId))
                .thenReturn(Mono.empty());

        //ACT & ASSERT
        StepVerifier.create(paperTenderService.getCostFromTenderId(tenderId, geokey, product))
                .expectErrorMatches((exception) -> {
                    assertInstanceOf(PnGenericException.class, exception);
                    assertEquals(TENDER_NOT_EXISTED.getMessage(), exception.getMessage());
                    return true;
                })
                .verify();

        Mockito.verify(pnPaperTenderDAO, times(1)).getTenderById(tenderId);
        Mockito.verify(pnPaperGeoKeyDAO, times(0)).getGeoKey(tenderId, product, geokey);
        Mockito.verify(pnPaperCostDAO, times(0)).getCostByTenderIdProductLotZone(tenderId, product, lot, zone);
    }

    @Test
    void getCostFromTenderIdThrowExceptionWhenGeokeyIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String product = "RS";
        String geokey = "00100";
        String lot = "1";
        String zone = "ZONE_1";

        Mockito.when(pnPaperTenderDAO.getTenderById(tenderId))
                .thenReturn(Mono.just(mockPnPaperTender(tenderId)));
        Mockito.when(pnPaperGeoKeyDAO.getGeoKey(tenderId, product, geokey))
                .thenReturn(Mono.empty());

        //ACT & ASSERT
        StepVerifier.create(paperTenderService.getCostFromTenderId(tenderId, geokey, product))
                .expectErrorMatches((exception) -> {
                    assertInstanceOf(PnGenericException.class, exception);
                    assertEquals(GEOKEY_NOT_FOUND.getMessage(), exception.getMessage());
                    return true;
                })
                .verify();

        Mockito.verify(pnPaperTenderDAO, times(1)).getTenderById(tenderId);
        Mockito.verify(pnPaperGeoKeyDAO, times(1)).getGeoKey(tenderId, product, geokey);
        Mockito.verify(pnPaperCostDAO, times(0)).getCostByTenderIdProductLotZone(tenderId, product, lot, zone);
    }

    @Test
    void getCostFromTenderIdThrowExceptionWhenCostIsNotFound() {
        //ARRANGE
        String tenderId = "GARA_2024";
        String product = "RS";
        String geokey = "00100";
        String lot = "LOT_1";
        String zone = "ZONE_1";

        Mockito.when(pnPaperTenderDAO.getTenderById(tenderId))
                .thenReturn(Mono.just(mockPnPaperTender(tenderId)));
        Mockito.when(pnPaperGeoKeyDAO.getGeoKey(tenderId, product, geokey))
                .thenReturn(Mono.just(mockPnPaperGeokey(tenderId)));
        Mockito.when(pnPaperCostDAO.getCostByTenderIdProductLotZone(tenderId, product, lot, zone))
                .thenReturn(Mono.empty());

        //ACT & ASSERT
        StepVerifier.create(paperTenderService.getCostFromTenderId(tenderId, geokey, product))
                .expectErrorMatches((exception) -> {
                    assertInstanceOf(PnGenericException.class, exception);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND.getMessage(), exception.getMessage());
                    return true;
                })
                .verify();

        Mockito.verify(pnPaperTenderDAO, times(1)).getTenderById(tenderId);
        Mockito.verify(pnPaperGeoKeyDAO, times(1)).getGeoKey(tenderId, product, geokey);
        Mockito.verify(pnPaperCostDAO, times(1)).getCostByTenderIdProductLotZone(tenderId, product, lot, zone);
    }

    private PnPaperChannelTender mockPnPaperTender() {
        PnPaperChannelTender mockTender = new PnPaperChannelTender();
        mockTender.setTenderId("TENDER_ID");
        mockTender.setActivationDate(Instant.now().minusSeconds(3600*24));
        mockTender.setTenderName("TENDER NAME");
        mockTender.setVat(22);
        mockTender.setNonDeductibleVat(10);
        mockTender.setPagePrice(BigDecimal.valueOf(0.09));
        mockTender.setBasePriceAR(BigDecimal.valueOf(0.6));
        mockTender.setBasePrice890(BigDecimal.valueOf(0.07));
        mockTender.setBasePriceRS(BigDecimal.valueOf(0.09));
        mockTender.setFee(BigDecimal.valueOf(0.19));
        mockTender.setCreatedAt(Instant.now());
        return mockTender;
    }

    private PnPaperChannelGeoKey mockPnPaperGeokey() {
        PnPaperChannelGeoKey mockGeokey = new PnPaperChannelGeoKey("TENDER_ID", "AR", "00001");
        mockGeokey.setActivationDate(Instant.now().minusSeconds(3600*24));
        mockGeokey.setLot("LOT_1");
        mockGeokey.setZone("Francia");
        mockGeokey.setCoverFlag(true);
        mockGeokey.setDismissed(false);
        mockGeokey.setCreatedAt(Instant.now().minusSeconds(3444));
        return mockGeokey;
    }

    private PnPaperChannelCost mockPnPaperCost() {
        PnPaperChannelCost mockCost = new PnPaperChannelCost("TENDER_ID", "AR", "LOT_1", "ZONE_1");
        mockCost.setDeliveryDriverId("DELIVERY_ID");
        mockCost.setDeliveryDriverName("DELIVERY_NAME");
        mockCost.setDematerializationCost(BigDecimal.valueOf(0.90));
        mockCost.setCreatedAt(Instant.now());
        return mockCost;
    }

    private void mockZone() {
        PnZone zone = new PnZone();
        zone.setZone("ZONE_1");
        zone.setCountryEn("Germany");
        zone.setCountryIt("Germania");
        when(zoneDAO.getByCountry("Germany")).thenReturn(Mono.just(zone));
    }

    private void mockTender() {
        tender = new PnTender();
        tender.setStatus("IN_PROGRESS");
        tender.setTenderCode("CODE_TENDER");
        when(tenderDAO.findActiveTender()).thenReturn(Mono.just(tender));
    }

    private void mockCost(){
        cost = new PnCost();
        cost.setFsu(false);
        cost.setCap(null);
        cost.setProductType("AR");
        cost.setPagePrice(BigDecimal.valueOf(1.23F));
        cost.setUuid("COST_UUID");
        cost.setZone("ZONE_1");
        cost.setBasePrice(BigDecimal.valueOf(1.43F));
        cost.setDeliveryDriverCode("DRIVER");
        cost.setTenderCode(tender.getTenderCode());
        when(costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(),  null, cost.getZone(), cost.getProductType()))
                .thenReturn(Mono.just(cost));
    }
}