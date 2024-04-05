package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventItem;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsConfig;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;

public class AttachmentsConfigMapper {

    private AttachmentsConfigMapper() {
    }


    public static PnAttachmentsConfig toPnAttachmentsConfig(String configKey, String configType, PnAttachmentsConfigEventItem pnConfigAttachmentsEventItem, String defaultCap) {
        if (pnConfigAttachmentsEventItem == null) {
            return null;
        }

        PnAttachmentsConfig config = new PnAttachmentsConfig();
        config.setConfigKey(AttachmentsConfigUtils.buildPartitionKey(configKey, configType));
        config.setStartValidity(pnConfigAttachmentsEventItem.getStartValidity());
        config.setEndValidity(pnConfigAttachmentsEventItem.getEndValidity());
        config.setCreatedAt(Instant.now());
        config.setRules(null);
        config.setParentReference(defaultCap);

        return config;
    }

    public static List<PnAttachmentsConfig> toPnAttachmentsConfig(String configKey, String configType, List<PnAttachmentsConfigEventItem> pnConfigAttachmentsEventItems, String defaultCap) {
        if (CollectionUtils.isEmpty(pnConfigAttachmentsEventItems)) {
            return List.of();
        }

        return pnConfigAttachmentsEventItems.stream()
                .map(item -> toPnAttachmentsConfig(configKey, configType, item, defaultCap))
                .toList();
    }
}
