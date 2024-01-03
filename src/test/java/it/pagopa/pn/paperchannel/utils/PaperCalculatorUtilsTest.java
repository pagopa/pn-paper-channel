package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
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

    @InjectMocks
    private PaperCalculatorUtils paperCalculatorUtils;
    @Mock
    private PaperTenderService paperTenderService;
    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;

    @Test
    void calculator() {
        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

        Mockito.when(pnPaperChannelConfig.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

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

        BigDecimal res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;
        Assertions.assertEquals(1, res.intValue());
    }

    //${peso busta} + ( ${numero di pagine} * ${peso pagina} )
    //nel test: 5 + (5 * 5) = 30 (numero di pagine 10, ma reversPrinter=true per cui 5 effettive)
    //${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )
    //nel test: 3 + (2 * 2) = 12 (essendo il peso 30, si va nel secondo range, dove: dto.setPrice50(BigDecimal.valueOf(2.00));)
    @Test
    void calculatorWithCOMPLETE() {

        List<AttachmentInfo> attachmentUrls = new ArrayList<>();
        AttachmentInfo pnAttachmentInfo = new AttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(10);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setCap("30030");

        Mockito.when(paperTenderService.getCostFrom(address.getCap(), null, ProductTypeEnum.AR.getValue()))
                .thenReturn(Mono.just(getNationalCost()));


        Mockito.when(pnPaperChannelConfig.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.COMPLETE);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(5);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(5);

        BigDecimal res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;
        Assertions.assertEquals(12, res.intValue());
    }

    @Test
    void calculatorInt() {
        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getInternationalCost()));
        //MOCK RETRIEVE ZONE FROM COUNTRY
        Mockito.when(paperTenderService.getZoneFromCountry(Mockito.any()))
                .thenReturn(Mono.just("ZONE_1"));

        Mockito.when(pnPaperChannelConfig.getChargeCalculationMode()).thenReturn(ChargeCalculationModeEnum.AAR);

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

        BigDecimal res = paperCalculatorUtils.calculator(attachmentUrls, address, ProductTypeEnum.AR, true).block();

        assert res != null;
        Assertions.assertEquals(223,  (int)(res.floatValue()*100));
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


        pnAttachmentInfo.setDocumentType("PN_AAR");
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
        return dto;
    }

    private CostDTO getInternationalCost() {
        CostDTO dto = new CostDTO();
        dto.setPrice(BigDecimal.valueOf(2.23));
        dto.setPriceAdditional(BigDecimal.valueOf(1.97));
        return dto;
    }
}