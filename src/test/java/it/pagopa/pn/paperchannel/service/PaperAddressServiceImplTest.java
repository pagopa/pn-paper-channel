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
    private SqsSender sqsSender;

    @BeforeEach
    public void init() {
        paperProperties = new PnPaperChannelConfig();
        var secondAttemptFlowHandlerFactory = new SecondAttemptFlowHandlerFactory(addressManagerClient, paperProperties);
        paperAddressService = new PaperAddressServiceImpl(null, null,
                sqsSender, null, addressDAO, secondAttemptFlowHandlerFactory);
    }

    @Test
    void getCorrectAddressNationalRegistryFlowOk() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setEqualityResult(false);
        mockDeduplicationResponse.setNormalizedAddress(new AnalogAddressDto().addressRow("via address-normalizzato"));
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

        Address expectedResponse = AddressMapper.fromAnalogAddressManager(mockDeduplicationResponse.getNormalizedAddress());

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, prepareAsyncRequest))
                .expectNext(expectedResponse).verifyComplete();

    }

    //Indirizzo coincidenti = D02
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationAddressEquals() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
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

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, prepareAsyncRequest))
                .expectErrorMatches(throwable -> {
                    boolean isPnUntracebleException = throwable instanceof PnUntracebleException;
                    PnUntracebleException pnUntracebleException = (PnUntracebleException) throwable;
                    boolean isD02 = pnUntracebleException.getKoReason().failureDetailCode() == FailureDetailCodeEnum.D02;
                    return isPnUntracebleException && isD02;
                })
                .verify();

    }

    //Errore PNADDR001: Indirizzo diverso - Normalizzazione KO = D01 (con configurazione Pnaddr01SendD01ToDeliveryPush = true)
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationError() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR001);
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

        paperProperties.setPnaddr001sendD01ToDeliveryPush(true);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, prepareAsyncRequest))
                .expectErrorMatches(throwable -> {
                    boolean isPnUntracebleException = throwable instanceof PnUntracebleException;
                    PnUntracebleException pnUntracebleException = (PnUntracebleException) throwable;
                    boolean isD01 = pnUntracebleException.getKoReason().failureDetailCode() == FailureDetailCodeEnum.D01;
                    boolean failedAddress = pnUntracebleException.getKoReason().addressFailed().equals(fromNationalRegistry);
                    return isPnUntracebleException && isD01 && failedAddress;
                })
                .verify();

    }

    //Indirizzo diverso - Normalizzazione KO con D01SendToDeliveryPush = false = non mando a delivery-push l'evento
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationErrorNoDeliveryPush() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError("an-error");
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

        paperProperties.setPnaddr001sendD01ToDeliveryPush(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, prepareAsyncRequest))
                .expectErrorMatches(throwable -> !(throwable instanceof PnUntracebleException))
                .verify();

    }

    //Indirizzo non trovato = D00 - da verificare in un caso reale
    @Test
    void getCorrectAddressNationalRegistryFlowKOForDeduplicationNull() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setNormalizedAddress(null);
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

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, fromNationalRegistry, prepareAsyncRequest))
                .expectErrorMatches(throwable -> throwable instanceof PnGenericException)
                .verify();

    }

    //Errore PNADDR001: Indirizzo diverso - Normalizzazione KO = D01 (con configurazione Pnaddr01SendD01ToDeliveryPush = true)
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR001() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR001);
        String requestId = "";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId("related-request");
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001sendD01ToDeliveryPush(true);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, prepareAsyncRequest))
                .expectErrorMatches(throwable -> {
                    boolean isPnUntracebleException = throwable instanceof PnUntracebleException;
                    PnUntracebleException pnUntracebleException = (PnUntracebleException) throwable;
                    boolean isD01 = pnUntracebleException.getKoReason().failureDetailCode() == FailureDetailCodeEnum.D01;
                    boolean failedAddress = pnUntracebleException.getKoReason().addressFailed().equals(addressDiscovered);
                    return isPnUntracebleException && isD01 && failedAddress;
                })
                .verify();

    }

    //Errore PNADDR002: (Indirizzo dichiarato postalizzabile dal normalizzatore ma con CAP/stato estero non abilitati (con configurazione Pnaddr02SendD01ToDeliveryPush = false)
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR002() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR002);
        String requestId = "";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId("related-request");
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001sendD01ToDeliveryPush(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, prepareAsyncRequest))
                .expectErrorMatches(throwable -> {
                    boolean isPnGenericException = throwable instanceof PnGenericException;
                    boolean isNotPnUntracebleException = !(throwable instanceof PnUntracebleException);
                    return isPnGenericException && isNotPnUntracebleException;
                })
                .verify();

    }

    //Errore PNADDR002: (Indirizzo dichiarato postalizzabile dal normalizzatore ma con CAP/stato estero non abilitati (con configurazione Pnaddr02SendD01ToDeliveryPush = false)
    @Test
    void getCorrectAddressDiscoveredAddressFlowKOForDeduplicationErrorPNADDR999() {
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        prepareAsyncRequest.setAttemptRetry(0);
        DeduplicatesResponseDto mockDeduplicationResponse = new DeduplicatesResponseDto();
        mockDeduplicationResponse.setError(PNADDR999);
        String requestId = "";
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("a-iun");
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setCorrelationId(null); //questo campo non-null determina il fatto di scegliere il flusso NR
        deliveryRequest.setRelatedRequestId("related-request");
        PnAddress addressFromDb = new PnAddress();
        addressFromDb.setRequestId(requestId);
        addressFromDb.setAddress("via Roma");
        PnAddress discoveredAddressFromDb = new PnAddress();
        discoveredAddressFromDb.setRequestId(requestId);
        discoveredAddressFromDb.setAddress("via discovered");

        PrepareAsyncRequest retryPrepareAsyncRequest = new PrepareAsyncRequest();
        retryPrepareAsyncRequest.setAttemptRetry(prepareAsyncRequest.getAttemptRetry());
        retryPrepareAsyncRequest.setIun(deliveryRequest.getIun());
        retryPrepareAsyncRequest.setRequestId(deliveryRequest.getRequestId());
        retryPrepareAsyncRequest.setCorrelationId(deliveryRequest.getCorrelationId());
        retryPrepareAsyncRequest.setAddressRetry(true);

        Address addressFirstAttempt = AddressMapper.toDTO(addressFromDb);
        Address addressDiscovered = AddressMapper.toDTO(discoveredAddressFromDb);

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS))
                .thenReturn(Mono.just(addressFromDb));

        when(addressDAO.findByRequestId(requestId, AddressTypeEnum.DISCOVERED_ADDRESS))
                .thenReturn(Mono.just(discoveredAddressFromDb));

        when(addressManagerClient.deduplicates(any(), eq(addressFirstAttempt), eq(addressDiscovered)))
                .thenReturn(Mono.just(mockDeduplicationResponse));

        paperProperties.setPnaddr001sendD01ToDeliveryPush(false);

        StepVerifier.create(paperAddressService.getCorrectAddress(deliveryRequest, null, prepareAsyncRequest))
                .expectErrorMatches(throwable -> {
                    boolean isPnAddressFlowException = throwable instanceof PnAddressFlowException;
                    PnAddressFlowException pnAddressFlowException = (PnAddressFlowException) throwable;
                    boolean isAddressManagerError = pnAddressFlowException.getExceptionType() == ExceptionTypeEnum.ADDRESS_MANAGER_ERROR; //ADDRESS_MANAGER_ERROR è retryable in coda
                    return isPnAddressFlowException && isAddressManagerError;
                })
                .verify();

        // verifico che scrivo in coda nuovamente l'evento di prepare async
        verify(sqsSender, times(1)).pushInternalError(retryPrepareAsyncRequest, retryPrepareAsyncRequest.getAttemptRetry(), PrepareAsyncRequest.class);

    }
}
