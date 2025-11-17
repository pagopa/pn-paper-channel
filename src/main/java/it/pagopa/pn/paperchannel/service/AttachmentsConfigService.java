package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AttachmentsConfigService {

    Mono<PnDeliveryRequest> filterAttachmentsToSend(PnDeliveryRequest pnDeliveryRequest, List<PnAttachmentInfo> attachmentInfoList, PnAddress pnAddress);
}
