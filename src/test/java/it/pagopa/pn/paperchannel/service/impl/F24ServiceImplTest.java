package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {PaperCalculatorUtils.class, F24ServiceImpl.class, PnAuditLogBuilder.class})
class F24ServiceImplTest {

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
    private SqsSender sqsSender;
    @MockBean
    private F24Client f24Client;

    @Test
    void checkDeliveryRequestAttachmentForF24() {
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest("REQUESTID", StatusDeliveryEnum.IN_PROCESSING);

        boolean res = f24Service.checkDeliveryRequestAttachmentForF24(pnDeliveryRequest);

        assertTrue(res);

        pnDeliveryRequest.getAttachments().get(0).setUrl("safestorage://filekey123");
        res = f24Service.checkDeliveryRequestAttachmentForF24(pnDeliveryRequest);
        assertFalse(res);
    }

    @Test
    void preparePDF() {
        String requestid = "REQUESTID";
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING);

        Mockito.when(pnPaperChannelConfig.getChargeCalculationMode()).thenReturn("AAR");
        Mockito.when(addressDAO.findByRequestId(requestid)).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
        Mockito.when(f24Client.preparePDF(requestid, pnDeliveryRequest.getIun(), "1", 200)).thenReturn(Mono.just(new RequestAcceptedDto()));
        //MOCK RETRIEVE NATIONAL COST
        Mockito.when(paperTenderService.getCostFrom(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(getNationalCost()));

        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
    }

    @Test
    void preparePDF_nocost() {
        String requestid = "REQUESTID";
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING);
        pnDeliveryRequest.getAttachments().get(0).setUrl("f24set://IUN123/1");

        Mockito.when(addressDAO.findByRequestId(requestid)).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
        Mockito.when(f24Client.preparePDF(requestid, pnDeliveryRequest.getIun(), "1", null)).thenReturn(Mono.just(new RequestAcceptedDto()));


        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
    }

    @Test
    void preparePDF_nocost_0() {
        String requestid = "REQUESTID";
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, StatusDeliveryEnum.IN_PROCESSING);
        pnDeliveryRequest.getAttachments().get(0).setUrl("f24set://IUN123/1?cost=0");

        Mockito.when(addressDAO.findByRequestId(requestid)).thenReturn(Mono.just(getPnAddress(requestid)));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));
        Mockito.when(f24Client.preparePDF(requestid, pnDeliveryRequest.getIun(), "1", 0)).thenReturn(Mono.just(new RequestAcceptedDto()));


        PnDeliveryRequest res = f24Service.preparePDF(pnDeliveryRequest).block();

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
    }


    @Test
    void arrangeF24AttachmentsAndReschedulePrepare() {
        String requestid = "REQUESTID";
        List<String> urls = List.of("safestorage://123456", "safestorage://9876543");
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(requestid, F24_WAITING);
        pnDeliveryRequest.getAttachments().get(0).setUrl("f24set://IUN123/1?cost=0");
        pnDeliveryRequest.getAttachments().get(0).setDocumentType("PN_F24_SET");

        Mockito.when(requestDeliveryDAO.getByRequestId(requestid)).thenReturn(Mono.just(pnDeliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        PnDeliveryRequest res = f24Service.arrangeF24AttachmentsAndReschedulePrepare(requestid, urls).block();

        assertEquals(F24_WAITING.getCode(), res.getStatusCode());
        assertEquals(2, res.getAttachments().size());
        Mockito.verify(this.sqsSender).pushToInternalQueue(Mockito.any());
    }

    @NotNull
    private PrepareAsyncRequest getPrepareAsyncRequest() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        prepareAsyncRequest.setRequestId("REQUESTID");
        prepareAsyncRequest.setIun("IUN123");
        prepareAsyncRequest.setAttemptRetry(1);
        return prepareAsyncRequest;
    }


    private PnDeliveryRequest getDeliveryRequest(String requestId, StatusDeliveryEnum status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("f24set://IUN123/1?cost=100");
        attachmentUrls.add(pnAttachmentInfo);


        deliveryRequest.setAddressHash(getAddress().convertToHash());
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("IUN123");
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
        dto.setPriceAdditional(BigDecimal.valueOf(2.00));
        return dto;
    }

}