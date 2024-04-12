package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ComplexProxy890MessageHandler extends Proxy890MessageHandler {

    @Override
    protected Boolean isComplexFlow(PaperProgressStatusEventDto paperRequest) {
        return true;
    }
}
