package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.model.PnPaperChannelRangeDTO;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.utils.config.CostRoundingModeConfig;
import it.pagopa.pn.paperchannel.utils.costutils.CostWithDriver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@ExtendWith(MockitoExtension.class)
class PaperCalculatorUtilsTest {
    private static final String DRIVER_CODE = "driverCode";
    private static final String TENDER_CODE = "tenderCode";

    @InjectMocks
    private PaperCalculatorUtils paperCalculatorUtils;
    @Mock
    private PaperTenderService paperTenderService;
    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;
    @Mock
    private DateChargeCalculationModesUtils dateChargeCalculationModesUtils;
    @Mock
    private CostRoundingModeConfig costRoundingModeConfig;


    @Test
    void testNationalWithReversePrinterAndProductTypeRS() {
        //Arrange
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(5);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        AttachmentInfo attachmentAAR = new AttachmentInfo();
        attachmentAAR.setDate("");
        attachmentAAR.setFileKey("http://localhost:8080");
        attachmentAAR.setId("");
        attachmentAAR.setNumberOfPage(5);
        attachmentAAR.setDocumentType(Const.PN_AAR);
        attachmentAAR.setUrl("");
        attachmentUrls.add(attachmentAAR);


        Address address = new Address();
        address.setCap("00100");
        address.setCountry("ITALIA");

        var costDTO = getPaperChannelCostDTO();

        costDTO.setProduct("RS");
        var isReversePrinter = true;

        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);


        Mockito.when(paperTenderService.getSimplifiedCost(address.getCap(), costDTO.getProduct()))
                .thenReturn(Mono.just(costDTO));


        //Act
        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RS, isReversePrinter).block();


        //Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.25).setScale(2, RoundingMode.UNNECESSARY), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());
    }

    @Test
    void testNationalWithReversePrinterAndProductTypeRSWithoutAAR() {
        //Arrange
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(5);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("00100");
        address.setCountry("ITALIA");

        var costDTO = getPaperChannelCostDTO();

        costDTO.setProduct("RS");

        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);


        Mockito.when(paperTenderService.getSimplifiedCost(address.getCap(), costDTO.getProduct()))
                .thenReturn(Mono.just(costDTO));

        //Act
        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RS, true).block();

        //Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(0.87), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());
    }

    @Test
    void testNationalOnlyFrontPrinterAndProductTypeRSWithoutAAR() {
        //Arrange 1° Caso
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(5);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("00100");
        address.setCountry("ITALIA");

        var costDTO = getPaperChannelCostDTO();

        costDTO.setProduct("RS");

        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(paperTenderService.getSimplifiedCost(address.getCap(), costDTO.getProduct()))
                .thenReturn(Mono.just(costDTO));

        //Act 1° Caso
        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RS, false).block();

        //Assert 1° Caso
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.12).setScale(2, RoundingMode.UNNECESSARY), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());


        //Arrange 2° Caso
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.UP);


        //Act 2° Caso
        res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RS, false).block();


        //Assert 2° Caso
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.13).setScale(2, RoundingMode.UNNECESSARY), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());
    }


    @Test
    void testInternationalOnlyFrontPrinterAndProductTypeRIRWithoutAAR() {
        //Arrange
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(5);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("ZONE_1");
        address.setCountry("BRASILE");

        var costDTO = getPaperChannelCostDTO();
        costDTO.setProduct("RIR");

        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(paperTenderService.getSimplifiedCost(address.getCountry(), costDTO.getProduct()))
                .thenReturn(Mono.just(costDTO));

        //Act
        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RIR, false).block();

        //Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.18).setScale(2, RoundingMode.UNNECESSARY), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());
    }


    @Test
    void testInternationalWithReversePrinterAndProductTypeRIS() {
        //Arrange
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(5);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        AttachmentInfo attachmentAAR = new AttachmentInfo();
        attachmentAAR.setDate("");
        attachmentAAR.setFileKey("http://localhost:8080");
        attachmentAAR.setId("");
        attachmentAAR.setNumberOfPage(5);
        attachmentAAR.setDocumentType(Const.PN_AAR);
        attachmentAAR.setUrl("");
        attachmentUrls.add(attachmentAAR);


        Address address = new Address();
        address.setCap("ZONE_1");
        address.setCountry("FRANCIA");

        var costDTO = getPaperChannelCostDTO();

        costDTO.setProduct("RIS");
        var isReversePrinter = true;

        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);

        Mockito.when(paperTenderService.getSimplifiedCost(address.getCountry(), costDTO.getProduct()))
                .thenReturn(Mono.just(costDTO));


        // Act
        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.RIS, isReversePrinter).block();


        //Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.25).setScale(2, RoundingMode.UNNECESSARY), res.getCost());
        Assertions.assertEquals(costDTO.getDeliveryDriverId(), res.getDriverCode());
        Assertions.assertEquals(costDTO.getTenderId(), res.getTenderCode());
    }


    @Test
    void testCalculatorSimulatorWithProductTypeRSAndReversePrinter() {
        // Arrange
        String geokey = "00100";

        var paperChannelCostDTO = getPaperChannelCostDTO();
        paperChannelCostDTO.setProduct("RS");

        var shipmentCalculateRequest = new ShipmentCalculateRequest();
        shipmentCalculateRequest.setGeokey(geokey);
        shipmentCalculateRequest.setIsReversePrinter(true);
        shipmentCalculateRequest.setPageWeight(5);
        shipmentCalculateRequest.setNumSides(12);
        shipmentCalculateRequest.setProduct(ShipmentCalculateRequest.ProductEnum.fromValue(paperChannelCostDTO.getProduct()));


        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(paperTenderService.getCostFromTenderId(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest.getGeokey(), paperChannelCostDTO.getProduct()))
                .thenReturn(Mono.just(paperChannelCostDTO));

        // Act
        ShipmentCalculateResponse res = paperCalculatorUtils.costSimulator(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest).block();

        // Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(125).intValue(), res.getCost());
    }

    @Test
    void testCalculatorSimulatorWithProductTypeRIRAndOnlyFront() {
        // Arrange
        String geokey = "BRASILE";

        var paperChannelCostDTO = getPaperChannelCostDTO();
        paperChannelCostDTO.setProduct("RIR");
        var shipmentCalculateRequest = new ShipmentCalculateRequest();
        shipmentCalculateRequest.setGeokey(geokey);
        shipmentCalculateRequest.setIsReversePrinter(false);
        shipmentCalculateRequest.setPageWeight(5);
        shipmentCalculateRequest.setNumSides(5);
        shipmentCalculateRequest.setProduct(ShipmentCalculateRequest.ProductEnum.fromValue(paperChannelCostDTO.getProduct()));

        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(paperTenderService.getCostFromTenderId(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest.getGeokey(), paperChannelCostDTO.getProduct()))
                .thenReturn(Mono.just(paperChannelCostDTO));

        // Act
        ShipmentCalculateResponse res = paperCalculatorUtils.costSimulator(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest).block();

        // Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(118).intValue(), res.getCost());
    }

    @Test
    void testCalculatorSimulatorWithProductTypeRISAndReversePrinter() {
        // Arrange
        String geokey = "FRANCIA";

        var paperChannelCostDTO = getPaperChannelCostDTO();
        paperChannelCostDTO.setProduct("RIS");
        var shipmentCalculateRequest = new ShipmentCalculateRequest();
        shipmentCalculateRequest.setGeokey(geokey);
        shipmentCalculateRequest.setIsReversePrinter(true);
        shipmentCalculateRequest.setPageWeight(5);
        shipmentCalculateRequest.setNumSides(12);
        shipmentCalculateRequest.setProduct(ShipmentCalculateRequest.ProductEnum.fromValue(paperChannelCostDTO.getProduct()));

        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);
        Mockito.when(costRoundingModeConfig.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        Mockito.when(paperTenderService.getCostFromTenderId(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest.getGeokey(), paperChannelCostDTO.getProduct()))
                .thenReturn(Mono.just(paperChannelCostDTO));

        // Act
        ShipmentCalculateResponse res = paperCalculatorUtils.costSimulator(paperChannelCostDTO.getTenderId(), shipmentCalculateRequest).block();

        // Assert
        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(125).intValue(), res.getCost());
    }



    @Test
    void calculatorAARNationalUsesCapAsGeokey() {
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

        Address address = new Address();
        address.setCap("30030");
        address.setCountry("ITALIA");

        var costDTO = getPaperChannelCostDTO();
        costDTO.setProduct(ProductTypeEnum.AR.getValue());
        costDTO.setBasePriceAR(BigDecimal.valueOf(1.00));
        costDTO.setDeliveryDriverId(DRIVER_CODE);
        costDTO.setTenderId(TENDER_CODE);

        Mockito.when(paperTenderService.getSimplifiedCost("30030", ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(costDTO));

        CostWithDriver res = paperCalculatorUtils.calculator(getAttachments(3), address, ProductTypeEnum.AR, true).block();

        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(1.00), res.getCost());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
        Mockito.verify(paperTenderService).getSimplifiedCost("30030", ProductTypeEnum.AR.getValue());
    }

    @Test
    void calculatorAARInternationalUsesCountryAsGeokey() {
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

        Address address = new Address();
        address.setCap("75001");
        address.setCountry("FRANCE");

        var costDTO = getPaperChannelCostDTO();
        costDTO.setProduct(ProductTypeEnum.RIR.getValue());
        costDTO.setBasePriceAR(BigDecimal.valueOf(2.23));
        costDTO.setDeliveryDriverId(DRIVER_CODE);
        costDTO.setTenderId(TENDER_CODE);

        Mockito.when(paperTenderService.getSimplifiedCost("FRANCE", ProductTypeEnum.RIR.getValue()))
                .thenReturn(Mono.just(costDTO));

        CostWithDriver res = paperCalculatorUtils.calculator(getAttachments(3), address, ProductTypeEnum.RIR, true).block();

        Assertions.assertNotNull(res);
        Assertions.assertEquals(BigDecimal.valueOf(2.23), res.getCost());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
        Mockito.verify(paperTenderService).getSimplifiedCost("FRANCE", ProductTypeEnum.RIR.getValue());
    }

    @Test
    void calculatorPropagatesErrorWhenCostNotFound() {
        Address address = new Address();
        address.setCap("30030");
        address.setCountry("ITALIA");

        Mockito.when(paperTenderService.getSimplifiedCost("30030", ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.COST_DRIVER_OR_FSU_NOT_FOUND,
                        ExceptionTypeEnum.COST_DRIVER_OR_FSU_NOT_FOUND.getMessage())));

        StepVerifier.create(paperCalculatorUtils.calculator(getAttachments(3), address, ProductTypeEnum.AR, true))
                .expectErrorMatches(ex -> ex instanceof PnGenericException pnEx
                        && pnEx.getExceptionType() == ExceptionTypeEnum.COST_DRIVER_OR_FSU_NOT_FOUND)
                .verify();

        Mockito.verifyNoInteractions(dateChargeCalculationModesUtils);
    }

    @Test
    void getLetterWeight() {
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(100);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(200);
        int res = paperCalculatorUtils.getLetterWeight(3, pnPaperChannelConfig.getPaperWeight(), pnPaperChannelConfig.getLetterWeight());
        assertEquals(700, res);
    }

    @Test
    void getNumberOfPages() {
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("PN_AAR1");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        int res = paperCalculatorUtils.getNumberOfPages(attachmentUrls, true, true);
        assertEquals(2, res);

        int res1 = paperCalculatorUtils.getNumberOfPages(attachmentUrls, false, true);
        assertEquals(3, res1);


        pnAttachmentInfo.setDocumentType(Const.PN_AAR);
        int res2 = paperCalculatorUtils.getNumberOfPages(attachmentUrls, false, false);
        assertEquals(2, res2);
        int res3 = paperCalculatorUtils.getNumberOfPages(attachmentUrls, false, true);
        assertEquals(3, res3);
    }

    @Test
    void getProposalProductType() {
        Address address = new Address();
        address.setCountry("FRANCE");

        String res = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum.RS.getValue());
        assertEquals("RIS", res);
        String ress = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum.AR.getValue());
        assertEquals("RIR", ress);
        String resss = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum._890.getValue());
        assertEquals("RIR", resss);

        address = new Address();
        address.setCountry("IT");
        address.setCap("10100");

        String res1 = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum.AR.getValue());
        assertEquals("AR", res1);
        String res2 = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum.RS.getValue());
        assertEquals("RS", res2);
        String res3 = paperCalculatorUtils.getProposalProductType(address, ProductTypeEnum._890.getValue());
        assertEquals("890", res3);
    }


    //(numero di pagine degli atti 4, ma reversPrinter=true per cui 2 effettive + 1 AAR)
    @Test
    void getNumberOfPagesIncludeAARTrueTest() {
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo aar = new AttachmentInfo();
        aar.setDate("");
        aar.setFileKey("http://localhost:8080");
        aar.setId("");
        aar.setNumberOfPage(1);
        aar.setDocumentType(Const.PN_AAR);
        aar.setUrl("");
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(4);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        attachmentUrls.add(aar);

        Integer numberOfPages = paperCalculatorUtils.getNumberOfPages(attachmentUrls, true, true);
        System.out.println(numberOfPages);
        assertEquals(3, numberOfPages);
    }

    //(numero di pagine degli atti 4, ma reversPrinter=true per cui 2 effettive)
    @Test
    void getNumberOfPagesIncludeAARFalseTest() {
        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo aar = new AttachmentInfo();
        aar.setDate("");
        aar.setFileKey("http://localhost:8080");
        aar.setId("");
        aar.setNumberOfPage(1);
        aar.setDocumentType(Const.PN_AAR);
        aar.setUrl("");
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(4);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        attachmentUrls.add(aar);

        Integer numberOfPages = paperCalculatorUtils.getNumberOfPages(attachmentUrls, true, false);
        System.out.println(numberOfPages);
        assertEquals(2, numberOfPages);
    }



    private List<AttachmentInfo> getAttachments(int numberOfPages) {
        AttachmentInfo attachment = new AttachmentInfo();
        attachment.setDocumentType("");
        attachment.setNumberOfPage(numberOfPages);
        return new ArrayList<>(List.of(attachment));
    }

    private PnPaperChannelCostDTO getPaperChannelCostDTO() {
        var dto = new PnPaperChannelCostDTO();

        dto.setTenderId("TENDER_ID");
        dto.setProductLotZone("PRODUCT_LOT_ZONE");
        dto.setProduct("RS");
        dto.setLot("23");
        dto.setZone("EU");
        dto.setDeliveryDriverName("Poste");
        dto.setDeliveryDriverId("POSTE");
        dto.setDematerializationCost(BigDecimal.valueOf(0.09876));
        dto.setVat(22);
        dto.setNonDeductibleVat(35);
        dto.setPagePrice(BigDecimal.valueOf(0.12345));
        dto.setBasePriceAR(BigDecimal.valueOf(0.05945));
        dto.setBasePriceRS(BigDecimal.valueOf(0.01234));
        dto.setBasePrice890(BigDecimal.valueOf(0.09432));
        dto.setFee(BigDecimal.valueOf(0.50025));


        var rangeDto1 = new PnPaperChannelRangeDTO();
        rangeDto1.setCost(BigDecimal.valueOf(0.00123));
        rangeDto1.setMinWeight(0);
        rangeDto1.setMaxWeight(20);

        var rangeDto2 = new PnPaperChannelRangeDTO();
        rangeDto2.setCost(BigDecimal.valueOf(0.00987));
        rangeDto2.setMinWeight(21);
        rangeDto2.setMaxWeight(50);

        var rangeDto3 = new PnPaperChannelRangeDTO();
        rangeDto3.setCost(BigDecimal.valueOf(0.01234));
        rangeDto3.setMinWeight(51);
        rangeDto3.setMaxWeight(100);

        var rangeDto4 = new PnPaperChannelRangeDTO();
        rangeDto4.setCost(BigDecimal.valueOf(0.09876));
        rangeDto4.setMinWeight(101);
        rangeDto4.setMaxWeight(250);

        var rangeDto5 = new PnPaperChannelRangeDTO();
        rangeDto5.setCost(BigDecimal.valueOf(0.12512));
        rangeDto5.setMinWeight(251);
        rangeDto5.setMaxWeight(350);

        var rangeDto6 = new PnPaperChannelRangeDTO();
        rangeDto6.setCost(BigDecimal.valueOf(0.43209));
        rangeDto6.setMinWeight(351);
        rangeDto6.setMaxWeight(1000);

        var rangeDto7 = new PnPaperChannelRangeDTO();
        rangeDto7.setCost(BigDecimal.valueOf(0.80123));
        rangeDto7.setMinWeight(1001);
        rangeDto7.setMaxWeight(2000);

        dto.setRangedCosts(List.of(rangeDto1, rangeDto2, rangeDto3, rangeDto4, rangeDto5, rangeDto6, rangeDto7));
        return dto;
    }

}