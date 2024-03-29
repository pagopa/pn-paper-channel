package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.paperchannel.mapper.AttachmentsConfigMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnAttachmentsConfigDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.service.AttachmentsConfigService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class AttachmentsConfigServiceImpl extends GenericService implements AttachmentsConfigService {

    private final PnAttachmentsConfigDAO pnAttachmentsConfigDAO;

    public AttachmentsConfigServiceImpl(SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO,
                                        PnAttachmentsConfigDAO pnAttachmentsConfigDAO) {
        super(sqsSender, requestDeliveryDAO);
        this.pnAttachmentsConfigDAO = pnAttachmentsConfigDAO;
    }


    public Mono<Void> refreshConfig(PnAttachmentsConfigEventPayload payload) {
        return Mono.defer(() -> {
            String configKey = payload.getConfigKey();
            String configType = payload.getConfigType();
            String partitionKey = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);
            var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, payload.getConfigs());
            return pnAttachmentsConfigDAO.putItemInTransaction(partitionKey, pnAttachmentsConfigs);
        });
    }
}
