package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.CheckCoverageAreaService;
import it.pagopa.pn.paperchannel.service.RaddAltService;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@ConditionalOnExpression(
        "'${pn.paper-channel.radd-coverage-search-mode}'.equals('LIGHT') or '${pn.paper-channel.radd-coverage-search-mode}'.equals('COMPLETE')"
)
@RequiredArgsConstructor
@Service
@Slf4j
public class CheckCoverageAreaRaddService implements CheckCoverageAreaService {
    private final PnPaperChannelConfig cfg;
    private final RaddAltService raddAltService;

    private static final String CODE_ACCEPTED = "DOC_TAG_ACCEPTED";
    private static final String CODE_REQUIRED = "DOC_TAG_REQUIRED";
    private static final String CODE_SKIPPED = "DOC_TAG_SKIPPED";
    private static final String AAR_TAG = "AAR";

    @Override
    public Mono<PnDeliveryRequest> filterAttachmentsToSend(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList, PnAddress pnAddress) {
        if (checkZipCoverage(pnDeliveryRequest, pnAddress)) {
            log.debug("Perform checkZipCoverage");
            return raddAltService.isAreaCovered(cfg.getRaddCoverageSearchMode(), pnAddress, pnDeliveryRequest.getNotificationSentAt())
                    .map(isCovered -> Boolean.TRUE.equals(isCovered)
                        ? applyFilter(pnDeliveryRequest, attachmentInfoList)
                        : sendAllAttachments(pnDeliveryRequest, attachmentInfoList)
                    );
        } else {
            log.debug("Skip checkZipCoverage, sending all attachments");
            return Mono.just(sendAllAttachments(pnDeliveryRequest, attachmentInfoList));
        }
    }

    private boolean checkZipCoverage(PnDeliveryRequest pnDeliveryRequest, PnAddress pnAddress) {
        return Boolean.TRUE.equals(pnDeliveryRequest.getAarWithRadd()) && Utility.isNational(pnAddress.getCountry());
    }

    private PnDeliveryRequest applyFilter(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList) {
        List<PnAttachmentInfo> toSend = new ArrayList<>();
        List<PnAttachmentInfo> toRemove = new ArrayList<>();
        for(PnAttachmentInfo attachment : attachmentInfoList) {
            String docTag = attachment.getDocTag();

            if (docTag == null || docTag.isBlank()) {
                setAttachmentResult(attachment, CODE_ACCEPTED, "Tag non presente viene considerato da inviare");
                toSend.add(attachment);
            } else if (AAR_TAG.equals(docTag)) {
                setAttachmentResult(attachment, CODE_REQUIRED, "Il tag " + docTag + " rientra tra quelli da inviare obbligatoriamente");
                toSend.add(attachment);
            } else {
                setAttachmentResult(attachment, CODE_SKIPPED, "Il tag " + docTag + " non rientra tra quelli da inviare");
                toRemove.add(attachment);
            }
        }

        pnDeliveryRequest.setAttachments(toSend);
        pnDeliveryRequest.setRemovedAttachments(toRemove);

        if (CollectionUtils.isEmpty(pnDeliveryRequest.getRemovedAttachments())) {
            log.info("filter hasn't removed attachments to send, sending all attachments list={}", pnDeliveryRequest.getAttachments());
        }
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

    private void setAttachmentResult(PnAttachmentInfo attachment, String code, String diagnostic) {
        attachment.setFilterResultCode(code);
        attachment.setFilterResultDiagnostic(diagnostic);
    }

    private PnDeliveryRequest sendAllAttachments(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList) {
        pnDeliveryRequest.setAttachments(attachmentInfoList);
        pnDeliveryRequest.setRemovedAttachments(new ArrayList<>());
        log.info("sending all attachments list={}", attachmentInfoList);
        return pnDeliveryRequest;
    }
}
