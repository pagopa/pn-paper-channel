package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.rest.v1.api.DeliveryDriverApi;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaperChannelRestV1Controller implements DeliveryDriverApi {
    @Autowired
    private PaperChannelService paperChannelService;


}
