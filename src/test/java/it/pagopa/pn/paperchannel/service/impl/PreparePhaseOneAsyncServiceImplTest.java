package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnUntracebleException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.RACCOMANDATA_SEMPLICE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreparePhaseOneAsyncServiceImplTest {

    @InjectMocks
    private PreparePhaseOneAsyncServiceImpl preparePhaseOneAsyncService;
    
    @Mock
    private PaperCalculatorUtils paperCalculatorUtils;

    @Mock
    private PaperAddressService paperAddressService;

    @Mock
    private AddressDAO addressDAO;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private AttachmentsConfigService attachmentsConfigService;
    @Mock
    private PrepareFlowStarter prepareFlowStarter;

    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Mock
    private PnPaperChannelConfig config;


    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;

    private final PrepareNormalizeAddressEvent request = new PrepareNormalizeAddressEvent();

    private final PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();

    @BeforeEach
    public void setUp(){
        inizialize();
    }

    @Test
    void preparePhaseOneAsyncAttemptZeroTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest =  getDeliveryRequest(requestId, iun);
        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .attempt(0)
                .build();

        var address = getAddress();

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        when(requestDeliveryDAO.getByRequestId(requestId, true)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, null, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(attachmentsConfigService.filterAttachmentsToSend(deliveryRequest, deliveryRequest.getAttachments(), addressEntity)).thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.updateData(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));


        StepVerifier.create(preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        //verifico che viene inviato l'evento di output della PREPARE fase 1
        verify(prepareFlowStarter, times(1)).pushPreparePhaseOneOutput(deliveryRequest, addressEntity);
        verify(prepareFlowStarter, never()).pushResultPrepareEvent(any(), any(), any(), any(), any());
    }

    @Test
    void preparePhaseOneAsyncAttemptOneTest() {
        var requestId = "PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1";
        var iun = "GJWA-HMEK-RGUJ-202307-H-1";
        var deliveryRequest =  getDeliveryRequest(requestId, iun);
        var address = getAddress();

        PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId(requestId)
                .iun(iun)
                .address(address)
                .attempt(0)
                .build();

        PnAddress addressEntity = AddressMapper.toEntity(address, deliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, config);

        when(requestDeliveryDAO.getByRequestId(requestId, true)).thenReturn(Mono.just(deliveryRequest));
        when(paperAddressService.getCorrectAddress(deliveryRequest, address, 0)).thenReturn(Mono.just(address));
        when(addressDAO.create(any(PnAddress.class))).thenReturn(Mono.just(addressEntity));
        when(attachmentsConfigService.filterAttachmentsToSend(deliveryRequest, deliveryRequest.getAttachments(), addressEntity)).thenReturn(Mono.just(deliveryRequest));
        when(requestDeliveryDAO.updateData(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));


        StepVerifier.create(preparePhaseOneAsyncService.preparePhaseOneAsync(event))
                .expectNext(deliveryRequest)
                .verifyComplete();

        //verifico che viene inviato l'evento di output della PREPARE fase 1
        verify(prepareFlowStarter, times(1)).pushPreparePhaseOneOutput(deliveryRequest, addressEntity);
        verify(prepareFlowStarter, never()).pushResultPrepareEvent(any(), any(), any(), any(), any());
    }



    @Test
    void preparePhaseOneAsyncTestErrorUntraceableAddress(){
        PnDeliveryRequest deliveryRequest = getDeliveryRequest("FATY-FATY-2023041520230302", "FATY-FATY-2023041520230302-101111");
        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.UNTRACEABLE;
        String statusCode = statusDeliveryEnum.getCode();
        String statusDescription = statusCode + " - " + statusDeliveryEnum.getDescription();
        String statusDetail = statusDeliveryEnum.getDetail();
        KOReason koReason = new KOReason(FailureDetailCodeEnum.D00, null);

        when(this.requestDeliveryDAO.getByRequestId(any(), anyBoolean()))
                .thenReturn(Mono.just(deliveryRequest));

        when(this.paperAddressService.getCorrectAddress(any(), any(), anyInt()))
                .thenReturn(Mono.error(new PnUntracebleException(koReason)));



        when(this.requestDeliveryDAO.updateStatus(eq(deliveryRequest.getRequestId()), eq(statusCode), eq(statusDescription), eq(statusDetail), any())).thenReturn(Mono.empty());

        doNothing().when(this.prepareFlowStarter).pushResultPrepareEvent(eq(deliveryRequest), isNull(), isNull(), eq(StatusCodeEnum.KO), eq(koReason));

        request.setCorrelationId("FFPAPERTEST.IUN_FATY");

        StepVerifier.create(this.preparePhaseOneAsyncService.preparePhaseOneAsync(request))
                .expectErrorMatches(ex -> {
                    assertInstanceOf(PnGenericException.class, ex);
                    return true;
                }).verify();

        // VERIFICO CHE IN QUESTO CASO NON VENGA MAI CREATO IL RECORD DI ERRORE
        verify(paperRequestErrorDAO, never()).created(any(PnRequestError.class));

        // VERIFICO CHE È STATO INVIATO L'EVENTO DI KOUNREACHABLE A DELIVERY PUSH
        verify(this.prepareFlowStarter, times(1)).pushResultPrepareEvent(eq(deliveryRequest), isNull(), isNull(), eq(StatusCodeEnum.KO), eq(koReason));
        verify(prepareFlowStarter, never()).pushPreparePhaseOneOutput(any(), any());
    }






    private void inizialize(){


    }

    private PnDeliveryRequest getDeliveryRequest(String requestId, String iun){
        final PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setIun(iun);
        deliveryRequest.setProposalProductType(RACCOMANDATA_SEMPLICE);
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        deliveryRequest.setAttachments(attachmentInfoList);
        return deliveryRequest;
    }


    private Address getAddress(){
        final Address address = new Address();
        address.setCap("20089");
        address.setCity("Milano");
        address.setCountry("Italia");
        address.setAddress("Via sottosopra");
        address.setPr("MI");
        address.setProductType(RACCOMANDATA_SEMPLICE);
        request.setRequestId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        request.setAddress(address);
        request.setAttempt(0);

        return address;
    }

    private List<PnAttachmentInfo> attachmentInfoList (PnDeliveryRequest deliveryRequest){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        attachmentInfo.setId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        attachmentInfo.setDate("2023-01-01T00:20:56.630714800Z");
        attachmentInfo.setUrl("");
        attachmentInfo.setDocumentType("pdf");
        attachmentInfo.setFileKey("http://localhost:8080");
        attachmentInfo.setNumberOfPage(0);
        attachmentInfoList.add(attachmentInfo);
        deliveryRequest.setAttachments(attachmentInfoList);
        return attachmentInfoList;
    }

    private List<PnAttachmentInfo> orderedAttachmentInfoList(){
        List<PnAttachmentInfo> attachmentInfoList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            PnAttachmentInfo attachment = new PnAttachmentInfo();
            attachment.setId("PAPERTEST.IUN-2023041520230302-101111.RECINDEX_0");
            attachment.setDate("2019-11-07T09:03:08Z");
            attachment.setUrl("http://1234" + (49-i));
            attachment.setDocumentType("pdf");
            attachment.setFileKey(String.valueOf((49-i)));
            attachment.setNumberOfPage(0);
            attachmentInfoList.add(attachment);
        }
        return attachmentInfoList;
    }

}
