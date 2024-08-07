package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.commons.rules.model.FilterChainResult;
import it.pagopa.pn.commons.rules.model.ListFilterChainResult;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.mapper.AttachmentsConfigMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnAttachmentsConfigDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsRule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rule.handler.PaperListChainEngine;
import it.pagopa.pn.paperchannel.service.AttachmentsConfigService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils.ZIPCODE_PK_PREFIX;


@Service
@Slf4j
public class AttachmentsConfigServiceImpl extends GenericService implements AttachmentsConfigService {

    private final PnAttachmentsConfigDAO pnAttachmentsConfigDAO;
    private final PaperListChainEngine paperListChainEngine;

    private final PnPaperChannelConfig pnPaperChannelConfig;


    public AttachmentsConfigServiceImpl(SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO,
                                        PnAttachmentsConfigDAO pnAttachmentsConfigDAO, PaperListChainEngine paperListChainEngine, PnPaperChannelConfig pnPaperChannelConfig) {
        super(sqsSender, requestDeliveryDAO);
        this.pnAttachmentsConfigDAO = pnAttachmentsConfigDAO;
        this.paperListChainEngine = paperListChainEngine;
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }


    public Mono<PnDeliveryRequest> filterAttachmentsToSend(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList, PnAddress pnAddress) {

        if (checkZipCoverage(pnDeliveryRequest, pnAddress)) {
            log.debug("Perform checkZipCoverage");
            return resolveRule(AttachmentsConfigUtils.buildPartitionKey(pnAddress.getCap(), ZIPCODE_PK_PREFIX), pnDeliveryRequest.getNotificationSentAt())
                    .defaultIfEmpty(List.of())
                    .flatMap(rules -> {
                                if (CollectionUtils.isEmpty(rules))
                                    return sendAllAttachments(pnDeliveryRequest, attachmentInfoList);
                                else
                                    return paperListChainEngine.filterItems(pnDeliveryRequest, attachmentInfoList, rules)
                                            .collectList()
                                            .map(attachmentFiltered -> sendFilteredAttachments(pnDeliveryRequest, attachmentFiltered));
                    } );
        } else {
            log.debug("Skip checkZipCoverage");
            return sendAllAttachments(pnDeliveryRequest, attachmentInfoList);
        }
    }

    private boolean checkZipCoverage(PnDeliveryRequest pnDeliveryRequest, PnAddress pnAddress) {
        return pnPaperChannelConfig.isEnabledocfilterruleengine()
                &&
                Boolean.TRUE.equals(pnDeliveryRequest.getAarWithRadd())
                &&
                Utility.isNational(pnAddress.getCountry());
    }

    @NotNull
    private PnDeliveryRequest sendFilteredAttachments(PnDeliveryRequest pnDeliveryRequest, List<ListFilterChainResult<PnAttachmentInfo>> attachmentFiltered) {

        pnDeliveryRequest.setAttachments(attachmentFiltered.stream().filter(FilterChainResult::isSuccess).map(this::enrichAttachmentInfoWithFilterResult).toList());
        pnDeliveryRequest.setRemovedAttachments(attachmentFiltered.stream().filter(x -> !x.isSuccess()).map(this::enrichAttachmentInfoWithFilterResult).toList());

        if (CollectionUtils.isEmpty(pnDeliveryRequest.getRemovedAttachments()))
            log.info("filter hasn't removed attachments to send, sending all attachments list={}", pnDeliveryRequest.getAttachments());
        else
        {
            log.info("filter has removed some attachments list={} removed={}", pnDeliveryRequest.getAttachments(), pnDeliveryRequest.getRemovedAttachments());
        }

        if (CollectionUtils.isEmpty(pnDeliveryRequest.getAttachments()))
        {
            log.error("filter has removed ALL documents, it's a misconfiguration");
            throw new PnInvalidChainRuleException("filter has removed ALL documents, it's a misconfiguration");
        }

        return pnDeliveryRequest;
    }

    @NotNull
    private PnAttachmentInfo enrichAttachmentInfoWithFilterResult(ListFilterChainResult<PnAttachmentInfo> res) {
        PnAttachmentInfo item = res.getItem();
        item.setFilterResultCode(res.getCode());
        item.setFilterResultDiagnostic(res.getDiagnostic());
        return item;
    }

    private Mono<PnDeliveryRequest> sendAllAttachments(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList) {
        pnDeliveryRequest.setAttachments(attachmentInfoList);
        pnDeliveryRequest.setRemovedAttachments(new ArrayList<>());
        log.info("sending all attachments list={}", attachmentInfoList);
        return Mono.just(pnDeliveryRequest);
    }

    private Mono<List<PnAttachmentsRule>> resolveRule(String key, Instant validityDate){
        return pnAttachmentsConfigDAO.findConfigInInterval(key, validityDate)
                .doOnNext(pnAttachmentsConfig -> log.info("AttachmentsConfig found for: {}, {}: {}", key, validityDate, pnAttachmentsConfig))
                .doOnDiscard(Object.class, o -> log.info("AttachmentsConfig not found for: {}, {}", key, validityDate))
                .flatMap(x -> {
                    // se le regole sono vuote, e il parent è popolato (e diverso dalla key), risalgo la catena
                    if (CollectionUtils.isEmpty(x.getRules()) && x.getParentReference() != null && !x.getParentReference().equals(key)) {
                        return resolveRule(x.getParentReference(), validityDate);
                    }else {
                        return Mono.just(CollectionUtils.isEmpty(x.getRules())?List.of():x.getRules());
                    }
                });
    }


    public Mono<Void> refreshConfig(PnAttachmentsConfigEventPayload payload) {
        return Mono.defer(() -> {
            String configKey = payload.getConfigKey();
            String configType = payload.getConfigType();
            String partitionKey = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);
            var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, payload.getConfigs(), pnPaperChannelConfig.getDefaultattachmentconfigcap());
            return pnAttachmentsConfigDAO.refreshConfig(partitionKey, pnAttachmentsConfigs);
        });
    }
}
