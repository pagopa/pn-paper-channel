package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ExternalChannelError {
    private PaperProgressStatusEventDto analogMail;
}
