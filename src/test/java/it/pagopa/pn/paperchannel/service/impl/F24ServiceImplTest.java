package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
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
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {PaperCalculatorUtils.class, F24ServiceImpl.class, PnAuditLogBuilder.class})
class F24ServiceImplTest {

    private final static String IUN = "ABCD-EFGH-00000000-000001";
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
    private SafeStorageClient safeStorageClient;
    @MockBean
    private HttpConnector httpConnector;

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
    @CsvSource(value = {
            "AAR, 5, 5, 10, 10, 100",
            "AAR, 12, 12, 10, 56, 100",
            "AAR, 12, 12, 0, 56, 100",
            "AAR, 12, 12, NULL, 56, 100",
            "COMPLETE, 5, 5, 10, 10, 4400",
            "COMPLETE, 4, 8, 10, 20, 6400",
            "COMPLETE, 4, 8, 10, 50, 12500",
            "COMPLETE, 4, 8, 0, 50, 12500",
            "COMPLETE, 4, 8, NULL, 50, 12500"
    }, nullValues = {"NULL"})
    @DisplayName("testPreparePDFSuccess")
    void testPreparePDFSuccess(String calculationMode, Integer paperWeight, Integer letterWeight, Integer f24Cost, Integer f24NumberOfPages, Integer expectedCost) throws IOException {

        // Given
        String requestId = "REQUESTID";

        Integer aarNumberOfPages = 1;
        Integer attachmentNumberOfPages = 10;

        ChargeCalculationModeEnum chargeCalculationModeEnum = ChargeCalculationModeEnum.valueOf(calculationMode);

        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestId, StatusDeliveryEnum.IN_PROCESSING, f24Cost);

        NumberOfPagesResponseDto numberOfPagesResponseDto = new NumberOfPagesResponseDto();
        numberOfPagesResponseDto.setNumberOfPages(f24NumberOfPages);

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
        Mockito.when(safeStorageClient.getFile(Mockito.any()))
                .thenReturn(Mono.just(aarFileDownloadResponse))
                .thenReturn(Mono.just(attachmentFileDownloadResponse));

        /* Called twice for AAR and other Attachment file, skipping F24 */
        Mockito.when(httpConnector.downloadFile(Mockito.any()))
                .thenReturn(Mono.just(aarDocument))
                .thenReturn(Mono.just(attachmentDocument));

        Mockito.when(f24Client.preparePDF(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Integer.class))).thenReturn(Mono.just(new RequestAcceptedDto()));

        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        // Then
        assertNotNull(res);
        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(expectedCost, res.getCost());

        /* Check called twice to verify F24 skip during attachment page calculation */
        if(calculationMode.equals(ChargeCalculationModeEnum.COMPLETE.name())) {
            Mockito.verify(safeStorageClient, Mockito.times(2)).getFile(Mockito.anyString());
            Mockito.verify(httpConnector, Mockito.times(2)).downloadFile(Mockito.anyString());
        }
        else {
            Mockito.verify(safeStorageClient, Mockito.never()).getFile(Mockito.anyString());
            Mockito.verify(httpConnector, Mockito.never()).downloadFile(Mockito.anyString());
        }

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

    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status, Integer f24Cost){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();

        String f24FileKey = f24Cost == null ? F24_FILE_KEY : String.format("%s?cost=%s", F24_FILE_KEY, f24Cost);

        PnAttachmentInfo f24PnAttachmentInfo = getPnAttachmentInfo(f24FileKey, Const.DOCUMENT_TYPE_F24_SET);
        PnAttachmentInfo aarPnAttachmentInfo = getPnAttachmentInfo("safestorage://PN_AAR-12345.pdf", Const.PN_AAR);
        PnAttachmentInfo notificationPnAttachmentInfo = getPnAttachmentInfo("safestorage://PN_NOTIFICATION_ATTACHMENTS-12345.pdf", ATTACHMENT_DOC_TYPE);

        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>(List.of(
                f24PnAttachmentInfo,
                aarPnAttachmentInfo,
                notificationPnAttachmentInfo)
        );

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
}