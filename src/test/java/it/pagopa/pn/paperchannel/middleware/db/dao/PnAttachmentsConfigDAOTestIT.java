package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsRule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRuleParams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PnAttachmentsConfigDAOTestIT extends BaseTest {

    @Autowired
    private PnAttachmentsConfigDAO pnAttachmentsConfigDAO;

    @Test
    void putItemAndFindAllByConfigKeyTest() {
        String geoKey = UUID.randomUUID().toString();
        String anotherGeoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOneFirstGeoKey = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2025-01-01T00:00:00.000Z");
        var pnAttachmentsConfigTwoFirstGeoKey = buildPnAttachmentsConfig(geoKey, "2025-01-02T00:00:00.000Z", "2025-02-01T00:00:00.000Z");
        var pnAttachmentsConfigSecondGeoKey = buildPnAttachmentsConfig(anotherGeoKey, "2023-01-02T00:00:00.000Z", "2023-02-01T00:00:00.000Z");

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOneFirstGeoKey).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigTwoFirstGeoKey).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigSecondGeoKey).block();

        final List<PnAttachmentsConfig> result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(2).isEqualTo(List.of(pnAttachmentsConfigOneFirstGeoKey, pnAttachmentsConfigTwoFirstGeoKey));
    }

    @Test
    void findMoreRecentByConfigKeyOneTest() {
        Instant notificationSentAt = Instant.parse("2024-01-10T00:00:00.000Z");
        String geoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOne = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");
        var pnAttachmentsConfigTwo = buildPnAttachmentsConfig(geoKey, "2024-01-12T00:00:00.000Z", "2024-02-01T23:59:59.000Z");
        var pnAttachmentsConfigThree = buildPnAttachmentsConfig(geoKey, "2024-02-02T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOne).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigTwo).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigThree).block();

        final PnAttachmentsConfig result = pnAttachmentsConfigDAO.findConfigInInterval(geoKey, notificationSentAt).block();

        assertThat(result).isNotNull().isEqualTo(pnAttachmentsConfigOne);
    }

    @Test
    void findMoreRecentByConfigKeyTwoTest() {
        Instant notificationSentAt = Instant.parse("2024-01-12T00:00:00.000Z");
        String geoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOne = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");
        var pnAttachmentsConfigTwo = buildPnAttachmentsConfig(geoKey, "2024-01-12T00:00:00.000Z", "2024-02-01T23:59:59.000Z");
        var pnAttachmentsConfigThree = buildPnAttachmentsConfig(geoKey, "2024-02-02T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOne).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigTwo).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigThree).block();

        final PnAttachmentsConfig result = pnAttachmentsConfigDAO.findConfigInInterval(geoKey, notificationSentAt).block();

        assertThat(result).isNotNull().isEqualTo(pnAttachmentsConfigTwo);
    }

    @Test
    void findMoreRecentByConfigKeyThreeTest() {
        Instant notificationSentAt = Instant.parse("2024-03-01T00:00:00.000Z");
        String geoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOne = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");
        var pnAttachmentsConfigTwo = buildPnAttachmentsConfig(geoKey, "2024-01-12T00:00:00.000Z", "2024-02-01T23:59:59.000Z");
        var pnAttachmentsConfigThree = buildPnAttachmentsConfig(geoKey, "2024-02-02T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOne).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigTwo).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigThree).block();

        final PnAttachmentsConfig result = pnAttachmentsConfigDAO.findConfigInInterval(geoKey, notificationSentAt).block();

        assertThat(result).isNotNull().isEqualTo(pnAttachmentsConfigThree);
    }

    @Test
    void refreshConfigTest() {
        String geoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOne = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");
        var pnAttachmentsConfigTwo = buildPnAttachmentsConfig(geoKey, "2024-01-12T00:00:00.000Z", "2024-02-01T23:59:59.000Z");
        var pnAttachmentsConfigThree = buildPnAttachmentsConfig(geoKey, "2024-02-02T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOne).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigTwo).block();
        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigThree).block();



        List<PnAttachmentsConfig> result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(3).isEqualTo(List.of(pnAttachmentsConfigOne, pnAttachmentsConfigTwo, pnAttachmentsConfigThree));

        var pnAttachmentsConfigNew = buildPnAttachmentsConfig(geoKey, "2024-01-02T00:00:00.000Z", "2024-02-02T00:00:00.000Z");
        var pnAttachmentsConfigNewTwo = buildPnAttachmentsConfig(geoKey, "2024-03-01T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.refreshConfig(geoKey, List.of(pnAttachmentsConfigNew, pnAttachmentsConfigNewTwo)).block();

        result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(2).isEqualTo(List.of(pnAttachmentsConfigNew, pnAttachmentsConfigNewTwo));

    }

    @Test
    void refreshConfigWithSameRecordsInPutAndDeleteTest() {
        String geoKey = UUID.randomUUID().toString();

        var pnAttachmentsConfigOne = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");

        pnAttachmentsConfigDAO.putItem(pnAttachmentsConfigOne).block();



        List<PnAttachmentsConfig> result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(1).isEqualTo(List.of(pnAttachmentsConfigOne));

        var pnAttachmentsConfigNew = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");
        var pnAttachmentsConfigNewTwo = buildPnAttachmentsConfig(geoKey, "2024-03-01T00:00:00.000Z", null);

        pnAttachmentsConfigDAO.refreshConfig(geoKey, List.of(pnAttachmentsConfigNew, pnAttachmentsConfigNewTwo)).block();

        result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(2).isEqualTo(List.of(pnAttachmentsConfigNew, pnAttachmentsConfigNewTwo));

    }

    @Test
    void refreshConfigWithNewRecordTest() {
        String geoKey = UUID.randomUUID().toString();

        List<PnAttachmentsConfig> result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).isEmpty();

        var pnAttachmentsConfigNew = buildPnAttachmentsConfig(geoKey, "2024-01-01T00:00:00.000Z", "2024-01-11T23:59:59.000Z");

        pnAttachmentsConfigDAO.refreshConfig(geoKey, List.of(pnAttachmentsConfigNew)).block();

        result = pnAttachmentsConfigDAO.findAllByConfigKey(geoKey).collectList().block();

        assertThat(result).hasSize(1).isEqualTo(List.of(pnAttachmentsConfigNew));

    }



    private PnAttachmentsConfig buildPnAttachmentsConfig(String configKey, String startValidity, String endValidity) {
        PnRuleParams params = new PnRuleParams();
        params.setTypeWithNextResult("AAR");
        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType("DOCUMENT_TYPE");
        rule.setParams(params);
        var pnAttachmentsConfig = new PnAttachmentsConfig();
        pnAttachmentsConfig.setConfigKey(configKey);
        pnAttachmentsConfig.setStartValidity(Instant.parse(startValidity));
        if(endValidity != null) {
            pnAttachmentsConfig.setEndValidity(Instant.parse(endValidity));
        }
        pnAttachmentsConfig.setCreatedAt(Instant.now());
        pnAttachmentsConfig.setUpdateAt(Instant.now());
        pnAttachmentsConfig.setRules(List.of(rule));
        return pnAttachmentsConfig;
    }

}
