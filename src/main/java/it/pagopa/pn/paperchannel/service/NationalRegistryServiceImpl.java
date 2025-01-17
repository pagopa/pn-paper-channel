package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.service.impl.GenericService;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_ERROR;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING;

@Service
@Slf4j
public class NationalRegistryServiceImpl extends GenericService implements NationalRegistryService {

    private final NationalRegistryClient nationalRegistryClient;
    private final PrepareFlowStarter prepareFlowStarter;

    public NationalRegistryServiceImpl(NationalRegistryClient nationalRegistryClient, SqsSender sqsSender,
                                       RequestDeliveryDAO requestDeliveryDAO, PrepareFlowStarter prepareFlowStarter) {
        super(sqsSender, requestDeliveryDAO);
        this.nationalRegistryClient = nationalRegistryClient;
        this.prepareFlowStarter = prepareFlowStarter;
    }

    public void finderAddressFromNationalRegistries(String requestId, String relatedRequestId, String fiscalCode,
                                                    String personType, String iun, Integer attempt) {

        PnLogAudit pnLogAudit = new PnLogAudit();

        String correlationId = Utility.buildNationalRegistriesCorrelationId(requestId);
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        MDCUtils.addMDCToContextAndExecute(Mono.delay(Duration.ofMillis(20)).publishOn(Schedulers.boundedElastic())
                .flatMap(i -> {
                    log.info("Start call national registries for find address with correlationId: {}", correlationId);
                    pnLogAudit.addsBeforeResolveService(iun, String.format("prepare requestId = %s, relatedRequestId= %s, correlationId = %s Request to National Registry service", requestId, relatedRequestId, correlationId));
                    return this.nationalRegistryClient.finderAddress(correlationId, fiscalCode, personType)
                            .onErrorResume(e -> {
                                NationalRegistryError error = new NationalRegistryError();
                                error.setIun(iun);
                                error.setMessage(e.getMessage());
                                error.setFiscalCode(fiscalCode);
                                error.setReceiverType(personType);
                                error.setRequestId(requestId);
                                error.setRelatedRequestId(relatedRequestId);
                                saveErrorAndPushError(requestId, NATIONAL_REGISTRY_ERROR, error, payload -> {
                                    prepareFlowStarter.redrivePreparePhaseOneAfterNationalRegistryError(payload, attempt);
                                    return null;
                                });
                                return Mono.error(e);
                            });
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(address -> {
                    log.info("National registries has response with correlationId: {}", correlationId);
                    return this.requestDeliveryDAO.getByRequestId(requestId)
                            .flatMap(entity -> {
                                log.debug("Entity edited with correlation id {} and new status {}", correlationId, NATIONAL_REGISTRY_WAITING.getDetail());
                                entity.setCorrelationId(correlationId);
                                return changeStatusDeliveryRequest(entity, NATIONAL_REGISTRY_WAITING);
                            });
                })
                .flatMap(Mono::just)
                .onErrorResume(ex -> {
                    pnLogAudit.addsFailResolveService(iun, String.format("prepare requestId = %s, relatedRequestId = %s, correlationId = %s Response KO from National Registry service", requestId, relatedRequestId, correlationId));
                    log.warn("NationalRegistries finder address with correlationId {} in errors {}", correlationId, ex.getMessage());
                    return Mono.empty();

                }).subscribeOn(Schedulers.boundedElastic())).subscribe();
    }
}
