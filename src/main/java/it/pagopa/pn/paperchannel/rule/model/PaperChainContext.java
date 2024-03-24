package it.pagopa.pn.paperchannel.rule.model;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class PaperChainContext extends ListChainContext<AttachmentInfo> {

    private PnDeliveryRequest request;

}
