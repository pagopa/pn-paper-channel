package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventItem;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.paperchannel.mapper.AttachmentsConfigMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnAttachmentsConfigDAO;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentsConfigServiceImplTest {

    @Mock
    private PnAttachmentsConfigDAO pnAttachmentsConfigDAO;

    @InjectMocks
    private AttachmentsConfigServiceImpl attachmentsConfigService;


    @Test
    void refreshConfigOk() {
        String configKey = "80100";
        String configType = ConfigTypeEnum.ZIPCODE.name();
        final String pk = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);

        PnAttachmentsConfigEventItem itemOne = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventItem itemTwo = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventPayload payload = PnAttachmentsConfigEventPayload.builder()
                .configKey(configKey)
                .configType(ConfigTypeEnum.ZIPCODE.name())
                .configs(List.of(itemOne, itemTwo))
                .build();

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo));

        when(pnAttachmentsConfigDAO.putItemInTransaction(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.empty());

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .verifyComplete();

        verify(pnAttachmentsConfigDAO).putItemInTransaction(pk, pnAttachmentsConfigs);

    }

    @Test
    void refreshConfigKOForDAO() {
        String configKey = "80100";
        String configType = ConfigTypeEnum.ZIPCODE.name();
        final String pk = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);

        PnAttachmentsConfigEventItem itemOne = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventItem itemTwo = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventPayload payload = PnAttachmentsConfigEventPayload.builder()
                .configKey(configKey)
                .configType(ConfigTypeEnum.ZIPCODE.name())
                .configs(List.of(itemOne, itemTwo))
                .build();

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo));

        when(pnAttachmentsConfigDAO.putItemInTransaction(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.error(new RuntimeException("Error DB")));

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .expectError(RuntimeException.class)
                .verify();

        verify(pnAttachmentsConfigDAO).putItemInTransaction(pk, pnAttachmentsConfigs);

    }


}
