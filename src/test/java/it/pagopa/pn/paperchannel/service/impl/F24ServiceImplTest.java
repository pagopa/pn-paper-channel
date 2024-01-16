package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.NumberOfPagesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {PaperCalculatorUtils.class, F24ServiceImpl.class, PnAuditLogBuilder.class})
class F24ServiceImplTest {

    private final static String IUN = "IUN123";
    private final static String F24_FILE_KEY = "f24set://IUN123/1";

    @Autowired
    private F24Service f24Service;

    @MockBean
    AddressDAO addressDAO;

    @MockBean
    RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private PaperTenderService paperTenderService;
    @MockBean
    private PnPaperChannelConfig pnPaperChannelConfig;
    @MockBean
    private DateChargeCalculationModesUtils dateChargeCalculationModesUtils;
    @MockBean
    private SqsSender sqsSender;
    @MockBean
    private F24Client f24Client;

    @Test
    @DisplayName("checkDeliveryRequestAttachmentForF24")
    void checkDeliveryRequestAttachmentForF24WithF24Attachment() {

        // Given
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest("REQUESTID", StatusDeliveryEnum.IN_PROCESSING, 0);

        // When
        boolean res = f24Service.checkDeliveryRequestAttachmentForF24(pnDeliveryRequest);

        // Then
        assertTrue(res);
    }

    @Test
    @DisplayName("checkDeliveryRequestAttachmentForF24")
    void checkDeliveryRequestAttachmentForF24WithoutF24Attachment() {

        // Given
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setAttachments(new ArrayList<>());

        // When
        boolean res = f24Service.checkDeliveryRequestAttachmentForF24(pnDeliveryRequest);

        // Then
        assertFalse(res);
    }

    @ParameterizedTest
    @CsvSource({
            "AAR, 5, 5, 10, 100",
            "AAR, 12, 12, 56, 100",
            "COMPLETE, 5, 5, 10, 4400",
            "COMPLETE, 4, 8, 20, 6400",
            "COMPLETE, 4, 8, 50, 12500"
    })
    @DisplayName("testPreparePDFSuccess")
    void testPreparePDFSuccess(String calculationMode, Integer paperWeight, Integer letterWeight, Integer numberOfPages, Integer expectedCost) {

        // Given
        String requestid = "REQUESTID";

        ChargeCalculationModeEnum chargeCalculationModeEnum = ChargeCalculationModeEnum.valueOf(calculationMode);

        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING, 10);

        NumberOfPagesResponseDto numberOfPagesResponseDto = new NumberOfPagesResponseDto();
        numberOfPagesResponseDto.setNumberOfPages(numberOfPages);

        // When
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(chargeCalculationModeEnum);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(paperWeight);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(letterWeight);

        Mockito.when(addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        Mockito.when(f24Client.getNumberOfPages(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(numberOfPagesResponseDto));
        Mockito.when(f24Client.preparePDF(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(Mono.just(new RequestAcceptedDto()));

        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        // Then
        assertNotNull(res);
        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(expectedCost, res.getCost());
    }

    @ParameterizedTest
    @EnumSource(ChargeCalculationModeEnum.class)
    @DisplayName("testPreparePDFWithNoCostSuccess")
    void testPreparePDFWithNoCostSuccess(ChargeCalculationModeEnum calculationMode) {

        // Given
        String requestid = "REQUESTID";
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING, null);
        pnDeliveryRequest.getAttachments().get(0).setFileKey("f24set://IUN123/1");

        NumberOfPagesResponseDto numberOfPagesResponseDto = new NumberOfPagesResponseDto();
        numberOfPagesResponseDto.setNumberOfPages(10);

        // When
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(calculationMode);
        Mockito.when(addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        Mockito.when(f24Client.getNumberOfPages(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(numberOfPagesResponseDto));
        Mockito.when(f24Client.preparePDF(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.just(new RequestAcceptedDto()));

        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        // Then
        assertNotNull(res);
        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
    }

    @ParameterizedTest
    @EnumSource(ChargeCalculationModeEnum.class)
    @DisplayName("testPreparePDFWithCostZeroSuccess")
    void testPreparePDFWithCostZeroSuccess(ChargeCalculationModeEnum calculationMode) {

        // Given
        String requestid = "REQUESTID";
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING, 0);
        pnDeliveryRequest.getAttachments().get(0).setFileKey("f24set://IUN123/1?cost=0");

        NumberOfPagesResponseDto numberOfPagesResponseDto = new NumberOfPagesResponseDto();
        numberOfPagesResponseDto.setNumberOfPages(10);

        // When
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(calculationMode);
        Mockito.when(addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        Mockito.when(f24Client.getNumberOfPages(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(numberOfPagesResponseDto));
        Mockito.when(f24Client.preparePDF(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.just(new RequestAcceptedDto()));

        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        // Then
        assertNotNull(res);
        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
    }

    @Test
    @DisplayName("testPreparePDFNumberOfPagesApiFail")
    void testPreparePDFNumberOfPagesApiFail() {

        // Given
        String requestid = "REQUESTID";

        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING, 10);

        // When
        Mockito.when(f24Client.getNumberOfPages(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(f24Service.preparePDF(pnDeliveryRequest))
                .expectErrorMatches((ex) -> {
                    assertInstanceOf(PnF24FlowException.class, ex);
                    return true;
                }).verify();
    }


    @Test
    @DisplayName("arrangeF24AttachmentsAndReschedulePrepare")
    void arrangeF24AttachmentsAndReschedulePrepare() {

        // Given
        String requestid = "REQUESTID";
        List<String> urls = List.of("safestorage://123456", "safestorage://9876543");
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, F24_WAITING, 0);

        // When
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        PnDeliveryRequest res = f24Service.arrangeF24AttachmentsAndReschedulePrepare(requestid, urls).block();

        // Then
        Mockito.verify(this.sqsSender).pushToInternalQueue(Mockito.any());

        assertNotNull(res);

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(4, res.getAttachments().size());

        assertTrue(res.getAttachments().stream().map(PnAttachmentInfo::getFileKey).toList().containsAll(urls));
        assertFalse(res.getAttachments().stream().map(PnAttachmentInfo::getFileKey).toList().contains(F24_FILE_KEY.concat("?cost=0")));
    }

    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status, Integer cost){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();

        String f24FileKey = cost == null ? F24_FILE_KEY : String.format("%s?cost=%s", F24_FILE_KEY, cost);

        PnAttachmentInfo f24PnAttachmentInfo = new PnAttachmentInfo();
        f24PnAttachmentInfo.setDate("");
        f24PnAttachmentInfo.setUrl("http://localhost:8080");
        f24PnAttachmentInfo.setId("");
        f24PnAttachmentInfo.setNumberOfPage(null);
        f24PnAttachmentInfo.setDocumentType(Const.DOCUMENT_TYPE_F24_SET);
        f24PnAttachmentInfo.setFileKey(f24FileKey);
        attachmentUrls.add(f24PnAttachmentInfo);

        PnAttachmentInfo aarPnAttachmentInfo = new PnAttachmentInfo();
        aarPnAttachmentInfo.setDate("");
        aarPnAttachmentInfo.setUrl("http://localhost:8080");
        aarPnAttachmentInfo.setId("");
        aarPnAttachmentInfo.setNumberOfPage(1);
        aarPnAttachmentInfo.setDocumentType(Const.PN_AAR);
        aarPnAttachmentInfo.setFileKey("safestorage://PN_AAR-12345.pdf");
        attachmentUrls.add(aarPnAttachmentInfo);

        PnAttachmentInfo notificationPnAttachmentInfo = new PnAttachmentInfo();
        notificationPnAttachmentInfo.setDate("");
        notificationPnAttachmentInfo.setUrl("http://localhost:8080");
        notificationPnAttachmentInfo.setId("");
        notificationPnAttachmentInfo.setNumberOfPage(10);
        notificationPnAttachmentInfo.setDocumentType("");
        notificationPnAttachmentInfo.setFileKey("safestorage://PN_NOTIFICATION_ATTACHMENTS-12345.pdf");
        attachmentUrls.add(notificationPnAttachmentInfo);

        deliveryRequest.setAddressHash(getAddress().convertToHash());
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun(IUN);
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode(status.getCode());
        deliveryRequest.setStatusDetail(status.getDetail());
        deliveryRequest.setStatusDescription(status.getDescription());
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash(deliveryRequest.getFiscalCode()));
        deliveryRequest.setProductType("AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }


    private Address getAddress(){
        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        address.setFromNationalRegistry(true);
        address.setFlowType(Const.PREPARE);
        return address;
    }


    private PnAddress getPnAddress(String requestId){
        PnAddress pnAddress = new PnAddress();
        pnAddress.setRequestId(requestId);
        pnAddress.setAddress("via roma");
        pnAddress.setAddressRow2("via lazio");
        pnAddress.setCap("00061");
        pnAddress.setCity("roma");
        pnAddress.setCity2("viterbo");
        pnAddress.setCountry("italia");
        pnAddress.setPr("PR");
        pnAddress.setFullName("Ettore Fieramosca");
        pnAddress.setNameRow2("Ettore");
        return pnAddress;
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
}