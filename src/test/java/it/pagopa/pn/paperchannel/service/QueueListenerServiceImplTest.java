package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.service.impl.QueueListenerServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;


class QueueListenerServiceImplTest extends BaseTest {
    @MockBean
    private PaperResultAsyncService paperResultAsyncService;
    @MockBean
    private PaperAsyncService paperAsyncService;
    @MockBean
    private AddressDAO addressDAO;

    @Autowired
    @SpyBean
    PnAuditLogBuilder auditLogBuilder;
    @MockBean
    RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    CostDAO costDAO;
    @MockBean
    NationalRegistryClient nationalRegistryClient;
    @MockBean
    SqsSender sqsSender;
    @Autowired
    QueueListenerServiceImpl queueListenerService;

    @BeforeEach
    public void setUp(){
        Mockito.when(this.requestDeliveryDAO.getByRequestId(Mockito.anyString())).thenReturn(Mono.just(new PnDeliveryRequest()));
        Mockito.when(this.requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(new PnDeliveryRequest()));
    }

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
    void nationalRegistriesResponseListenerUntraceableAddressTest(){
       try{
           this.queueListenerService.nationalRegistriesResponseListener(new AddressSQSMessageDto());
           Assertions.fail("Il metodo non è andato in eccezione");
       }
        catch(PnGenericException ex){
            Assertions.assertEquals(UNTRACEABLE_ADDRESS, ex.getExceptionType());
        }

        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("");
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(UNTRACEABLE_ADDRESS, ex.getExceptionType());
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
        Mockito.when(this.addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getRelatedAddress()));
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