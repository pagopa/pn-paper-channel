package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;


class PnPaperChannelGeoKeyTest {
    private Instant activationDate;
    private String tenderId;
    private String product;
    private String geokey;
    private String lot;
    private String zone;
    private Boolean coverFlag;
    private Boolean dismissed;
    private Instant createdAt;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void getProductLotZoneTest() {
        // ARRANGE
        PnPaperChannelGeoKey channelGeoKey = initPaperChannelGeoKey();

        // ACT
        String tenderProductGeokey = channelGeoKey.getTenderProductGeokey();

        // ASSERT
        Assertions.assertNotNull(channelGeoKey);
        Assertions.assertEquals(String.join("#", tenderId, product, geokey), tenderProductGeokey);
    }

    private PnPaperChannelGeoKey initPaperChannelGeoKey() {
        PnPaperChannelGeoKey channelGeoKey = new PnPaperChannelGeoKey(tenderId, product, geokey);
        channelGeoKey.setActivationDate(activationDate);
        channelGeoKey.setLot(lot);
        channelGeoKey.setZone(zone);
        channelGeoKey.setCoverFlag(coverFlag);
        channelGeoKey.setDismissed(dismissed);
        channelGeoKey.setCreatedAt(createdAt);
        return channelGeoKey;
    }

    private void initialize() {
        activationDate = Instant.now();
        tenderId = "tenderId";
        product = "AR";
        geokey = "geokey";
        lot = "A";
        zone = "ZONE_1";
        coverFlag = false;
        dismissed = false;
        createdAt = Instant.now();
    }
}