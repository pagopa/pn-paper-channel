package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventItem;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventPayload;
import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.impl.QueueListenerServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@ExtendWith(MockitoExtension.class)
class QueueListenerServiceImplTest extends BaseTest{

    @Mock
    private PaperResultAsyncService paperResultAsyncService;
    @Mock
    private PaperAsyncService paperAsyncService;
    @Mock
    private AddressDAO addressDAO;

    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Spy
    private PnAuditLogBuilder auditLogBuilder;
    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    private CostDAO costDAO;
    @Mock
    private NationalRegistryClient nationalRegistryClient;
    @Mock
    private SqsSender sqsSender;
    @Mock
    private F24Service f24Service;
    @InjectMocks
    QueueListenerServiceImpl queueListenerService;


    @Test
    void internalListenerTest(){
        Mockito.when(this.paperAsyncService.prepareAsync(Mockito.any())).thenReturn(Mono.just(new PnDeliveryRequest()));
        this.queueListenerService.internalListener(new PrepareAsyncRequest(),10);
    }

    @Test
    void internalListenerErrorTest(){
        Mockito.when(this.paperAsyncService.prepareAsync(Mockito.any())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION,ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage() )));
        try{
            this.queueListenerService.internalListener(new PrepareAsyncRequest(),10);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(PREPARE_ASYNC_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseListenerUntraceableAddressBecauseCorrelationIdIsNotFoundTest(){
        try{
            this.queueListenerService.nationalRegistriesResponseListener(new AddressSQSMessageDto());
            Assertions.fail("Il metodo non è andato in eccezione");
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(CORRELATION_ID_NOT_FOUND, ex.getExceptionType());
        }

        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("");
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(CORRELATION_ID_NOT_FOUND, ex.getExceptionType());
        }
    }
    @Test
    void nationalRegistriesResponseListenerDeliveryNotExistsTest (){
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.empty());
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("1234");
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(DELIVERY_REQUEST_NOT_EXIST, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseListenerReceivedDeliveryRequestErrormessageisnotemptyforcorrelationId(){
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("1234");
        addressSQSMessageDto.setError("ok");
        AddressSQSMessagePhysicalAddressDto addressDto = new AddressSQSMessagePhysicalAddressDto();
        addressSQSMessageDto.setPhysicalAddress(addressDto);
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(NATIONAL_REGISTRY_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseOk(){
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(this.addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getRelatedAddress()));
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("1234");
        AddressSQSMessagePhysicalAddressDto addressDto = new AddressSQSMessagePhysicalAddressDto();
        addressSQSMessageDto.setPhysicalAddress(addressDto);
        this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);

    }

    @Test
    void nationalRegistriesResponseListenerReceivedDeliveryRequestKoLogTest(){
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("1234");
        addressSQSMessageDto.setError("");
        this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
    }


    @Test
    void f24ResponseListenerOKTest(){
        PnF24PdfSetReadyEvent response = PnF24PdfSetReadyEvent.builder()
                .detail(PnF24PdfSetReadyEvent.Detail.builder()
                        .clientId("cxid")
                        .pdfSetReady(PnF24PdfSetReadyEventPayload.builder()
                                .requestId("requestid123")
                                .generatedPdfsUrls(List.of(PnF24PdfSetReadyEventItem.builder()
                                                .uri("safestorage://2345678")
                                        .build(),
                                        PnF24PdfSetReadyEventItem.builder()
                                                .uri("safestorage://876543")
                                                .build()))
                                .build())
                        .build())
                .build();

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        Mockito.when(this.f24Service.arrangeF24AttachmentsAndReschedulePrepare(Mockito.eq("requestid123"), Mockito.anyList())).thenReturn(Mono.just(deliveryRequest));

        Assertions.assertDoesNotThrow(() -> this.queueListenerService.f24ResponseListener(response));

    }


    @Test
    void f24ResponseListenerKOTest(){
        PnF24PdfSetReadyEvent response = PnF24PdfSetReadyEvent.builder()
                .detail(PnF24PdfSetReadyEvent.Detail.builder()
                        .clientId("cxid")
                        .pdfSetReady(PnF24PdfSetReadyEventPayload.builder()
                                .generatedPdfsUrls(List.of(PnF24PdfSetReadyEventItem.builder()
                                                .uri("safestorage://2345678")
                                                .build(),
                                        PnF24PdfSetReadyEventItem.builder()
                                                .uri("safestorage://876543")
                                                .build()))
                                .build())
                        .build())
                .build();

        Assertions.assertThrows(PnGenericException.class, () -> this.queueListenerService.f24ResponseListener(response));

    }

    @Test
    void f24ErrorListenerOKTest(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));

        Assertions.assertDoesNotThrow(() -> this.queueListenerService.f24ErrorListener(error, 1));

    }

    @Test
    void f24ErrorListenerKOTest(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(new PnInternalException("missing URL f24set on f24serviceImpl", PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR)));
        //Mockito.doNothing().when(sqsSender).pushInternalError(Mockito.any(F24Error.class), Mockito.anyInt(), Mockito.eq(F24Error.class));

        Assertions.assertThrows(PnF24FlowException.class, () -> this.queueListenerService.f24ErrorListener(error, 1));
        Mockito.verify(requestDeliveryDAO, Mockito.timeout(1000)).updateData(Mockito.any());

    }

    @Test
    void f24ErrorListenerKOTest2(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(new PnF24FlowException(ExceptionTypeEnum.F24_ERROR, error, new NullPointerException())));
        //Mockito.doNothing().when(sqsSender).pushInternalError(Mockito.any(F24Error.class), Mockito.any(Integer.class), Mockito.eq(F24Error.class));

        Assertions.assertThrows(PnF24FlowException.class, () -> this.queueListenerService.f24ErrorListener(error, 1));
        Mockito.verify(requestDeliveryDAO, Mockito.timeout(1000)).updateData(Mockito.any());

    }

    /*@Test
    void nationalRegistriesErrorListenerOkTest(){
        Mockito.when(this.nationalRegistryClient.finderAddress(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(new AddressOKDto()));

       // this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),10);
    }*/

    /*@Test
    void nationalRegistriesErrorListenerErrorTest(){
        Mockito.when(this.paperResultAsyncService.resultAsyncBackground(Mockito.any(), Mockito.any())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION,ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage() )));
        try{
            this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),10);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(ex.getExceptionType(),PREPARE_ASYNC_LISTENER_EXCEPTION);
        }
    }*/

    @Test
    void externalChannelListenerOkTest(){
        Mockito.when(this.paperResultAsyncService.resultAsyncBackground(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),10);
    }

    @Test
    void externalChannelListenerErrorTest(){
        Mockito.when(this.paperResultAsyncService.resultAsyncBackground(Mockito.any(), Mockito.any())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION,ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage() )));
        try{
            this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),10);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    private PnAddress getRelatedAddress(){
        PnAddress address = new PnAddress();
        address.setFullName("Mario Rossi");
        return address;
    }
}