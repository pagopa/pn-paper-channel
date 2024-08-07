package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.utils.costutils.CostWithDriver;
import org.apache.poi.ss.formula.functions.T;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void calculator() {
        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("30030");

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(1, res.getCost().intValue());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }

    //${peso busta} + ( ${numero di pagine} * ${peso pagina} )
    //nel test: 5 + (1 * 5) = 10
    //${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )
    //nel test: 1 + (0 * 2) = 1 (essendo il peso 10, si va nel primo range, dove: dto.setPrice(BigDecimal.valueOf(1.00));)
    @Test
    void calculatorWithOneCOMPLETE() {

        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(1);
        pnAttachmentInfo.setDocumentType(Const.PN_AAR);
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("30030");

        Mockito.when(paperTenderService.getCostFrom(address.getCap(), null, ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(getNationalCost()));


        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(1, res.getCost().intValue());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }

    //${peso busta} + ( ${numero di pagine} * ${peso pagina} )
    //nel test: 5 + (6 * 5) = 35 (numero di pagine degli atti 9, ma reversPrinter=true per cui 5 effettive + 1 AAR)
    //${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )
    //nel test: 2 + (5 * 2) = 12 (essendo il peso 35, si va nel secondo range, dove: dto.setPrice50(BigDecimal.valueOf(2.00));)
    @Test
    void calculatorWithCOMPLETE() {

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
        pnAttachmentInfo.setNumberOfPage(9);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        attachmentUrls.add(aar);

        Address address = new Address();
        address.setCap("30030");

        Mockito.when(paperTenderService.getCostFrom(address.getCap(), null, ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(getNationalCost()));


        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(12, res.getCost().intValue());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }


    //${peso busta} + ( ${numero di pagine} * ${peso pagina} )
    //nel test: 5 + (9 * 5) = 50 (numero di pagine degli atti 15, ma reversPrinter=true per cui 8 effettive + 1 AAR)
    //${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )
    //nel test: 2 + (8 * 2) = 18 (essendo il peso 55, si va nel terzo range, dove: dto.setPrice50(BigDecimal.valueOf(2.00));)
    @Test
    void calculatorWithSecondRangeLimitUpCOMPLETE() {

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
        pnAttachmentInfo.setNumberOfPage(15);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        attachmentUrls.add(aar);

        Address address = new Address();
        address.setCap("30030");

        Mockito.when(paperTenderService.getCostFrom(address.getCap(), null, ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(getNationalCost()));


        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(18, res.getCost().intValue());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }

    //${peso busta} + ( ${numero di pagine} * ${peso pagina} )
    //nel test: 5 + (10 * 5) = 55 (numero di pagine degli atti 17, ma reversPrinter=true per cui 9 effettive + 1 AAR)
    //${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )
    //nel test: 3 + (9 * 2) = 21 (essendo il peso 55, si va nel terzo range, dove: dto.setPrice100(BigDecimal.valueOf(3.00));)
    @Test
    void calculatorWithThirdRangeCOMPLETE() {

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
        pnAttachmentInfo.setNumberOfPage(17);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);
        attachmentUrls.add(aar);

        Address address = new Address();
        address.setCap("30030");

        Mockito.when(paperTenderService.getCostFrom(address.getCap(), null, ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(getNationalCost()));


        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(21, res.getCost().intValue());
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }

    @Test
    void calculatorInt() {
        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getInternationalCost()));
        //MOCK RETRIEVE ZONE FROM COUNTRY
        Mockito.when(paperTenderService.getZoneFromCountry(Mockito.any()))
                .thenReturn(Mono.just("ZONE_1"));

        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCountry("FRANCE");

        CostWithDriver res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;

        Assertions.assertEquals(223,  (int)(res.getCost().floatValue()*100));
        Assertions.assertEquals(DRIVER_CODE, res.getDriverCode());
        Assertions.assertEquals(TENDER_CODE, res.getTenderCode());
    }

    @Test
    void getLetterWeight() {
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(100);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(200);
        int res = paperCalculatorUtils.getLetterWeight(3);
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
    void getProductType() {
        Address address = new Address();
        address.setCountry("FRANCE");

        String res = paperCalculatorUtils.getProductType(address, ProductTypeEnum.RIR);
        assertEquals("AR", res);

        String ress = paperCalculatorUtils.getProductType(address, ProductTypeEnum._890);
        assertEquals("AR", ress);

        String resss = paperCalculatorUtils.getProductType(address, ProductTypeEnum.RIS);
        assertEquals("RS", resss);

        address = new Address();
        address.setCountry("IT");
        address.setCap("10100");

        String res1 = paperCalculatorUtils.getProductType(address, ProductTypeEnum.AR);
        assertEquals("AR", res1);
        String res2 = paperCalculatorUtils.getProductType(address, ProductTypeEnum.RS);
        assertEquals("RS", res2);
        String res3 = paperCalculatorUtils.getProductType(address, ProductTypeEnum._890);
        assertEquals("890", res3);
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



    private CostDTO getNationalCost() {
        CostDTO dto = new CostDTO();
        dto.setPrice(BigDecimal.valueOf(1.00));
        dto.setPrice50(BigDecimal.valueOf(2.00));
        dto.setPrice100(BigDecimal.valueOf(3.00));
        dto.setPrice250(BigDecimal.valueOf(4.00));
        dto.setPrice350(BigDecimal.valueOf(5.00));
        dto.setPrice1000(BigDecimal.valueOf(6.00));
        dto.setPrice2000(BigDecimal.valueOf(7.00));
        dto.setPriceAdditional(BigDecimal.valueOf(2.00));
        dto.setDriverCode(DRIVER_CODE);
        dto.setTenderCode(TENDER_CODE);
        return dto;
    }

    private CostDTO getInternationalCost() {
        CostDTO dto = new CostDTO();
        dto.setPrice(BigDecimal.valueOf(2.23));
        dto.setPriceAdditional(BigDecimal.valueOf(1.97));
        dto.setDriverCode(DRIVER_CODE);
        dto.setTenderCode(TENDER_CODE);
        return dto;
    }
}