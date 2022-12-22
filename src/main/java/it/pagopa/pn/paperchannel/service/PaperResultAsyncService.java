package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;

public interface PaperResultAsyncService {
    String resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto);
}
