package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.MetadataPagesDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.NumberOfPagesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
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
import it.pagopa.pn.paperchannel.service.SafeStorageService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {PaperCalculatorUtils.class, F24ServiceImpl.class, PnAuditLogBuilder.class})
class F24ServiceImplTest {

    private final static String IUN = "ABCD-EFGH-00000000-000001";
    private final static String RECIPIENT_INDEX = "0";
    private final static String F24_FILE_KEY = "f24set://ABCD-EFGH-00000000-000001/0";
    private final static String ATTACHMENT_DOC_TYPE = "PDF";
    private final static String ATTACHMENT_URL = "http://localhost:8080";

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
    @MockBean
    private SafeStorageService safeStorageService;

    @Test
    @DisplayName("checkDeliveryRequestAttachmentForF24")
    void checkDeliveryRequestAttachmentForF24WithF24Attachment() {

        // Given
        List<PnAttachmentInfo> attachmentUrls = this.getPnAttachmentInfoList(null, null);
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest("REQUESTID", StatusDeliveryEnum.IN_PROCESSING, attachmentUrls);

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
    @MethodSource(value = "preparePDFTestCases")
    @DisplayName("testPreparePDFSuccess")
    void testPreparePDFSuccess(
            String calculationMode,
            Integer paperWeight,
            Integer letterWeight,
            Integer f24Cost,
            Integer vat,
            Integer expectedAnalogCost,
            Integer expectedF24CostWithVat
    ) throws IOException {

        // Given
        String requestId = "REQUESTID";

        Integer aarNumberOfPages = 1;
        Integer attachmentNumberOfPages = 10;

        ChargeCalculationModeEnum chargeCalculationModeEnum = ChargeCalculationModeEnum.valueOf(calculationMode);

        List<PnAttachmentInfo> attachmentUrls = this.getPnAttachmentInfoList(f24Cost, vat);
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestId, StatusDeliveryEnum.IN_PROCESSING, attachmentUrls);

        NumberOfPagesResponseDto numberOfPagesResponseDto = new NumberOfPagesResponseDto();
        numberOfPagesResponseDto.setF24Set(getF24MetadataPages(10));

        FileDownloadResponseDto aarFileDownloadResponse = getFileDownloadDTOResponse(Const.PN_AAR);
        FileDownloadResponseDto attachmentFileDownloadResponse = getFileDownloadDTOResponse(ATTACHMENT_DOC_TYPE);

        PDDocument aarDocument = getPDDocumentWithPages(aarNumberOfPages);
        PDDocument attachmentDocument = getPDDocumentWithPages(attachmentNumberOfPages);

        // When
        Mockito.when(dateChargeCalculationModesUtils.getChargeCalculationMode()).thenReturn(chargeCalculationModeEnum);
        Mockito.when(pnPaperChannelConfig.getPaperWeight()).thenReturn(paperWeight);
        Mockito.when(pnPaperChannelConfig.getLetterWeight()).thenReturn(letterWeight);

        Mockito.when(addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getPnAddress(requestId)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        Mockito.when(f24Client.getNumberOfPages(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(numberOfPagesResponseDto));

        /* Called twice for AAR and other Attachment file, skipping F24 */
        Mockito.when(safeStorageService.getFileRecursive(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(aarFileDownloadResponse))
                .thenReturn(Mono.just(attachmentFileDownloadResponse));

        /* Called twice for AAR and other Attachment file, skipping F24 */
        Mockito.when(safeStorageService.downloadFile(Mockito.any()))
                .thenReturn(Mono.just(aarDocument))
                .thenReturn(Mono.just(attachmentDocument));

        Mockito.when(f24Client.preparePDF(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Integer.class))).thenReturn(Mono.just(new RequestAcceptedDto()));

        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        // Then
        assertNotNull(res);
        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(expectedAnalogCost, res.getCost());

        /* Verify that F24 file generation is performed with exact cost including VAT when specified:
           partialCost + (analogCost + analogCost * vat/100)
        */
        Mockito.verify(f24Client).preparePDF(requestId, IUN, RECIPIENT_INDEX, expectedF24CostWithVat);

        /* Check called twice to verify F24 skip during attachment page calculation */
        if(calculationMode.equals(ChargeCalculationModeEnum.COMPLETE.name()) && f24Cost != null && f24Cost > 0) {
            Mockito.verify(safeStorageService, Mockito.times(2)).getFileRecursive(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(safeStorageService, Mockito.times(2)).downloadFile(Mockito.anyString());
        }
        else {
            Mockito.verify(safeStorageService, Mockito.never()).getFileRecursive(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(safeStorageService, Mockito.never()).downloadFile(Mockito.anyString());
        }

    }

    @Test
    @DisplayName("testPreparePDFNumberOfPagesApiFail")
    void testPreparePDFNumberOfPagesApiFail() {

        // Given
        String requestid = "REQUESTID";

        List<PnAttachmentInfo> attachmentUrls = this.getPnAttachmentInfoList(10, null);
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING, attachmentUrls);

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
        List<String> f24SetResolvedUrls = List.of("safestorage://123456", "safestorage://9876543");

        List<PnAttachmentInfo> attachmentUrls = this.getPnAttachmentInfoList(null, null);
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, F24_WAITING, attachmentUrls);

        // When
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        PnDeliveryRequest res = f24Service.arrangeF24AttachmentsAndReschedulePrepare(requestid, f24SetResolvedUrls).block();

        // Then
        Mockito.verify(this.sqsSender).pushToInternalQueue(Mockito.any());

        assertNotNull(res);

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(4, res.getAttachments().size());

        assertTrue(res.getAttachments().stream().map(PnAttachmentInfo::getFileKey).toList().containsAll(f24SetResolvedUrls));
        assertFalse(res.getAttachments().stream().map(PnAttachmentInfo::getFileKey).anyMatch(fileKey -> fileKey.startsWith(F24ServiceImpl.URL_PROTOCOL_F24)));
    }

    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status, List<PnAttachmentInfo> attachmentUrls){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();

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

    private List<MetadataPagesDto> getF24MetadataPages(Integer count) {

        List<MetadataPagesDto> f24MetadataPagesDtoList = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            MetadataPagesDto metadataPagesDto = new MetadataPagesDto();
            metadataPagesDto.setNumberOfPages(1);
            metadataPagesDto.setFileKey(RandomStringUtils.randomAscii(10));

            f24MetadataPagesDtoList.add(metadataPagesDto);
        }

        return f24MetadataPagesDtoList;
    }

    private List<PnAttachmentInfo> getPnAttachmentInfoList(Integer f24Cost, Integer vat) {

        UriComponentsBuilder f24Uri = UriComponentsBuilder.fromUriString(F24_FILE_KEY);

        if (f24Cost != null) f24Uri.queryParam("cost", f24Cost);
        if (vat != null) f24Uri.queryParam("vat", vat);

        String f24FileKey = f24Uri.toUriString();

        /* F24 Set Attachment */
        PnAttachmentInfo f24PnAttachmentInfo = getPnAttachmentInfo(f24FileKey, Const.DOCUMENT_TYPE_F24_SET);

        PnAttachmentInfo aarPnAttachmentInfo = getPnAttachmentInfo("safestorage://PN_AAR-12345.pdf", Const.PN_AAR);
        PnAttachmentInfo notificationPnAttachmentInfo = getPnAttachmentInfo("safestorage://PN_NOTIFICATION_ATTACHMENTS-12345.pdf", ATTACHMENT_DOC_TYPE);

        return new ArrayList<>(List.of(
                f24PnAttachmentInfo,
                aarPnAttachmentInfo,
                notificationPnAttachmentInfo)
        );
    }

    private PnAttachmentInfo getPnAttachmentInfo(String fileKey, String documentType) {

        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate(Instant.now().toString());
        pnAttachmentInfo.setUrl(ATTACHMENT_URL);
        pnAttachmentInfo.setId(RandomStringUtils.randomAscii(10));
        pnAttachmentInfo.setFileKey(fileKey);
        pnAttachmentInfo.setDocumentType(documentType);

        return pnAttachmentInfo;
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

    private FileDownloadResponseDto getFileDownloadDTOResponse(String documentType) {

        FileDownloadResponseDto fileDownloadResponseDto = new FileDownloadResponseDto();
        FileDownloadInfoDto download = new FileDownloadInfoDto();

        download.setUrl("safestorage://url");

        fileDownloadResponseDto.setDocumentType(documentType);
        fileDownloadResponseDto.setKey("http://localhost:8080");
        fileDownloadResponseDto.setChecksum("checksum");

        fileDownloadResponseDto.setDownload(download);

        return fileDownloadResponseDto;
    }

    private PDDocument getPDDocumentWithPages(Integer numberOfPages) throws IOException {
        try (PDDocument pdDocument = new PDDocument()) {
            for (int n=0; n<numberOfPages; n++) {
                pdDocument.addPage(new PDPage());
            }

            return pdDocument;
        }
    }

    /** 
     * Build test argument cases for {@link F24ServiceImplTest#testPreparePDFSuccess}
     * */
    private static Stream<Arguments> preparePDFTestCases() {

        /* Test cases for AAR date calculation mode */
        Arguments aarTestCaseWithNoVat1 = Arguments.of("AAR", 5, 5, 10, null, 100, 110);
        Arguments aarTestCaseWithNoVat2 = Arguments.of("AAR", 12, 12, 10, null, 100, 110);
        Arguments aarTestCaseWithZeroCostAndNoVat = Arguments.of("AAR", 12, 12, 0, null, null, null);
        Arguments aarTestCaseWithNoCostAndNoVat = Arguments.of("AAR", 12, 12, null, null, null, null);
        Arguments aarTestCaseWithCostAndVat = Arguments.of("AAR", 5, 5, 10, 22, 100, 132);

        /* Test cases for COMPLETE date calculation mode */
        Arguments completeTestCaseWithNoVat1 = Arguments.of("COMPLETE", 5, 5, 10, null, 6400, 6410);
        Arguments completeTestCaseWithNoVat2 = Arguments.of("COMPLETE", 1, 1, 10, null, 6200, 6210);
        Arguments completeTestCaseWithZeroCostAndNoVat = Arguments.of("COMPLETE", 4, 8, 0, null, null, null);
        Arguments completeTestCaseWithNoCostAndNoVat = Arguments.of("COMPLETE", 4, 8, null, null, null, null);
        Arguments completeTestCaseWithCostAndVat = Arguments.of("COMPLETE", 5, 5, 10, 22, 6400, 7818);

        return Stream.of(
                aarTestCaseWithNoVat1,
                aarTestCaseWithNoVat2,
                aarTestCaseWithZeroCostAndNoVat,
                aarTestCaseWithNoCostAndNoVat,
                aarTestCaseWithCostAndVat,
                completeTestCaseWithNoVat1,
                completeTestCaseWithNoVat2,
                completeTestCaseWithZeroCostAndNoVat,
                completeTestCaseWithNoCostAndNoVat,
                completeTestCaseWithCostAndVat
        );

    }
}