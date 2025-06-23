package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnAddressFlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnUntracebleException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnaddressmanager.v1.dto.DeduplicatesResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.AddressManagerClient;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.impl.PaperAddressServiceImpl;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION;
import static it.pagopa.pn.paperchannel.utils.DeduplicateErrorConst.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperAddressServiceImplTest {

    private PaperAddressServiceImpl paperAddressService;

    private PnPaperChannelConfig paperProperties;
    @Mock
    private AddressDAO addressDAO;
    @Mock
    private AddressManagerClient addressManagerClient;

    @Mock
    private NationalRegistryService nationalRegistryService;

    @Mock
    private PrepareFlowStarter prepareFlowStarter;

    @Mock
    private RequestDeliveryDAO requestDeliveryDAO;

    @BeforeEach
    void init() {
        paperProperties = new PnPaperChannelConfig();
        var secondAttemptFlowHandlerFactory = new SecondAttemptFlowHandlerFactory(addressManagerClient, paperProperties);
        paperAddressService = new PaperAddressServiceImpl(requestDeliveryDAO, null, addressDAO, secondAttemptFlowHandlerFactory, nationalRegistryService, prepareFlowStarter);
    }

    @Deprecated
    @Test
    void getCorrectAddressForFirstAttemptFlow() {
        String requestId = "";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));


        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, 0))
                .expectNext(addressFirstAttempt).verifyComplete();

        verify(addressManagerClient, never()).deduplicates(anyString(), any(Address.class), any(Address.class));
    }


    @Deprecated
    @Test
    void getCorrectAddressNationalRegistryFlowOk() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setEqualityResult(false);
        mockDeduplicationResponse.setNormalizedAddress(new AnalogAddressDto().addressRow("via address-normalizzato"));
        Address fromNationalRegistry = new Address();
        fromNationalRegistry.setAddress("via Da NR");
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        String correlationId = "a-correlation-id";
        deliveryRequest.setCorrelationId(correlationId); //questo campo non-null determina il fatto di scegliere il flusso NR
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressManagerClient.deduplicates(correlationId, addressFirstAttempt, fromNationalRegistry))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        Address expectedResponse = AddressMapper.fromAnalogAddressManager(mockDeduplicationResponse.getNormalizedAddress());

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, 0))
                .expectNext(expectedResponse).verifyComplete();

    }


    //Flusso NR: Indirizzo coincidenti, mando a delivery-push l'evento D02
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationAddressEqualsNrFlow() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setEqualityResult(true);
        Address fromNationalRegistry = new Address();
        fromNationalRegistry.setAddress("via Da NR");
        String requestId = "";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        String correlationId = "a-correlation-id";
        deliveryRequest.setCorrelationId(correlationId); //questo campo non-null determina il fatto di scegliere il flusso NR
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressManagerClient.deduplicates(correlationId, addressFirstAttempt, fromNationalRegistry))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, 0))
                .expectErrorMatches(throwable -> {
                    boolean isPnUntracebleException = throwable instanceof PnUntracebleException;
                    PnUntracebleException pnUntracebleException = (PnUntracebleException) throwable;
                    boolean isD02 = pnUntracebleException.getKoReason().failureDetailCode() == FailureDetailCodeEnum.D02;
                    return isPnUntracebleException && isD02;
                })
                .verify();

    }

    //Flusso postino: Indirizzo coincidenti, richiamo i registri nazionali
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationAddressEqualsPostmanFlow() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setEqualityResult(true);
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        when(requestDeliveryDAO.getByRequestId(requestId, true))
                .thenReturn(Mono.just(deliveryRequest));

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, addressDiscovered, 0))
                .expectErrorMatches(throwable ->
                   throwable instanceof PnAddressFlowException ex && ex.getExceptionType() == ExceptionTypeEnum.ATTEMPT_ADDRESS_NATIONAL_REGISTRY
                )
                .verify();

    }

    //Errore PNADDR001 flusso NR: Indirizzo non postalizzabile - invio a delivery-push il D01 (con configurazione Pnaddr001ContinueFlow = true)
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationError() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR001);
        Address fromNationalRegistry = new Address();
        fromNationalRegistry.setAddress("via Da NR");
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        String correlationId = "a-correlation-id";
        deliveryRequest.setCorrelationId(correlationId); //questo campo non-null determina il fatto di scegliere il flusso NR
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressManagerClient.deduplicates(correlationId, addressFirstAttempt, fromNationalRegistry))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001continueFlow(true);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, 0))
                .expectErrorMatches(throwable -> {
                    boolean isPnUntracebleException = throwable instanceof PnUntracebleException;
                    PnUntracebleException pnUntracebleException = (PnUntracebleException) throwable;
                    boolean isD01 = pnUntracebleException.getKoReason().failureDetailCode() == FailureDetailCodeEnum.D01;
                    boolean failedAddress = pnUntracebleException.getKoReason().addressFailed().equals(fromNationalRegistry);
                    return isPnUntracebleException && isD01 && failedAddress;
                })
                .verify();

    }

    //Errore non gestito flusso NR - Scrivo sulla tabella degli errori (lancio di PnGenericException(RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION))
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationErrorNoDeliveryPush() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError("an-error");
        Address fromNationalRegistry = new Address();
        fromNationalRegistry.setAddress("via Da NR");
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        String correlationId = "a-correlation-id";
        deliveryRequest.setCorrelationId(correlationId); //questo campo non-null determina il fatto di scegliere il flusso NR
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressManagerClient.deduplicates(correlationId, addressFirstAttempt, fromNationalRegistry))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001continueFlow(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, 0))
                .expectErrorMatches(throwable ->
                    throwable instanceof PnGenericException ex && ex.getExceptionType() == RESPONSE_ERROR_NOT_HANDLED_FROM_DEDUPLICATION
                )
                .verify();

    }

    //Indirizzo non trovato = D00 - da verificare in un caso reale
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationNull() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setNormalizedAddress(null);
        Address fromNationalRegistry = new Address();
        fromNationalRegistry.setAddress("via Da NR");
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        String correlationId = "a-correlation-id";
        deliveryRequest.setCorrelationId(correlationId); //questo campo non-null determina il fatto di scegliere il flusso NR
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressManagerClient.deduplicates(correlationId, addressFirstAttempt, fromNationalRegistry))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, 0))
                .expectErrorMatches(PnGenericException.class::isInstance)
                .verify();

    }

    //Errore PNADDR001 flusso postino: Indirizzo non postalizzabile, lancio di PnAddressFlowException(ATTEMPT_ADDRESS_NATIONAL_REGISTRY)
    // per richiamare i servizi nazionali (con configurazione Pnaddr001ContinueFlow = true)
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR001() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR001);
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        when(requestDeliveryDAO.getByRequestId(requestId, true))
                .thenReturn(Mono.just(deliveryRequest));

        paperProperties.setPnaddr001continueFlow(true);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, 0))
                .expectErrorMatches(throwable -> {
                    boolean isPnAddressFlowException = throwable instanceof PnAddressFlowException;
                    PnAddressFlowException pnAddressFlowException = (PnAddressFlowException) throwable;
                    boolean errorToRunNRFlow = pnAddressFlowException.getExceptionType() == ExceptionTypeEnum.ATTEMPT_ADDRESS_NATIONAL_REGISTRY;
                    return isPnAddressFlowException && errorToRunNRFlow;
                })
                .verify();

    }

    //Errore PNADDR002 flusso postino: (Indirizzo dichiarato postalizzabile dal normalizzatore ma con CAP/stato estero non abilitati (con configurazione Pnaddr002ContinueFlow = false)
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR002() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR002);
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001continueFlow(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, 0))
                .expectErrorMatches(throwable -> {
                    boolean isPnGenericException = throwable instanceof PnGenericException;
                    boolean isNotPnUntracebleException = !(throwable instanceof PnUntracebleException);
                    return isPnGenericException && isNotPnUntracebleException;
                })
                .verify();

    }

    //Errore PNADDR999 flusso postino: errore dal normalizzatore, lancio PnAddressFlowException(ADDRESS_MANAGER_ERROR)
    // per effettuare delle reties in coda
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR999() {
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR999);
        String requestId = "";
        String relatedRequestId = "related-request";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setRelatedRequestId(relatedRequestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId("related-request");
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        PrepareAsyncRequest retryPrepareAsyncRequest = new PrepareAsyncRequest();
        retryPrepareAsyncRequest.setAttemptRetry(0);
        retryPrepareAsyncRequest.setIun(deliveryRequest.getIun());
        retryPrepareAsyncRequest.setRequestId(deliveryRequest.getRequestId());
        retryPrepareAsyncRequest.setCorrelationId(deliveryRequest.getCorrelationId());
        retryPrepareAsyncRequest.setAddressRetry(true);

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(relatedRequestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001continueFlow(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, 0))
                .expectErrorMatches(throwable -> {
                    boolean isPnAddressFlowException = throwable instanceof PnAddressFlowException;
                    PnAddressFlowException pnAddressFlowException = (PnAddressFlowException) throwable;
                    boolean isAddressManagerError = pnAddressFlowException.getExceptionType() == ExceptionTypeEnum.ADDRESS_MANAGER_ERROR; //ADDRESS_MANAGER_ERROR è retryable in coda
                    return isPnAddressFlowException && isAddressManagerError;
                })
                .verify();

        // verifico che scrivo in coda nuovamente l'evento di prepare async
        verify(prepareFlowStarter, times(1)).redrivePreparePhaseOneAfterAddressManagerError(deliveryRequest, retryPrepareAsyncRequest.getAttemptRetry(), null);

    }
}
