package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.RaddSearchMode;
import it.pagopa.pn.paperchannel.service.RaddAltService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckCoverageAreaRaddServiceTest {

    private PnPaperChannelConfig cfg;
    private RaddAltService raddAltService;
    private CheckCoverageAreaRaddService service;

    @BeforeEach
    void setUp() {
        cfg = mock(PnPaperChannelConfig.class);
        raddAltService = mock(RaddAltService.class);
        service = new CheckCoverageAreaRaddService(cfg, raddAltService);
    }

    @Test
    void testRefreshConfigReturnsEmpty() {
        Mono<Void> result = service.refreshConfig(mock(PnAttachmentsConfigEventPayload.class));
        assertTrue(result.blockOptional().isEmpty());
    }

    @Test
    void testFilterAttachmentsToSend_AreaCovered_FiltersCorrectly() {
        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.LIGHT);
        when(raddAltService.isAreaCovered(any(), any(), any())).thenReturn(Mono.just(true));

        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);
        req.setNotificationSentAt(Instant.now());

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("AAR");
        PnAttachmentInfo info2 = new PnAttachmentInfo();
        info2.setDocTag("ALTRO");

        List<PnAttachmentInfo> attachments = Arrays.asList(info1, info2);

        PnDeliveryRequest result = service.filterAttachmentsToSend(req, attachments, address).block();

        assertNotNull(result);
        assertEquals(1, result.getAttachments().size());
        assertEquals("AAR", result.getAttachments().getFirst().getDocTag());
        assertEquals(1, result.getRemovedAttachments().size());
        assertEquals("ALTRO", result.getRemovedAttachments().getFirst().getDocTag());
    }

    @Test
    void testFilterAttachmentsToSend_AreaNotCovered_SendsAll() {
        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.valueOf("COMPLETE"));
        when(raddAltService.isAreaCovered(any(), any(), any())).thenReturn(Mono.just(false));

        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);
        req.setNotificationSentAt(Instant.now());

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("AAR");
        PnAttachmentInfo info2 = new PnAttachmentInfo();
        info2.setDocTag("ALTRO");

        List<PnAttachmentInfo> attachments = Arrays.asList(info1, info2);

        PnDeliveryRequest result = service.filterAttachmentsToSend(req, attachments, address).block();

        assertNotNull(result);
        assertEquals(2, result.getAttachments().size());
        assertEquals(0, result.getRemovedAttachments().size());
    }

    @Test
    void testFilterAttachmentsToSend_SkipCheckZipCoverage() {
        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(false);

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("AAR");
        List<PnAttachmentInfo> attachments = List.of(info1);

        PnDeliveryRequest result = service.filterAttachmentsToSend(req, attachments, address).block();

        assertNotNull(result);
        assertEquals(1, result.getAttachments().size());
        assertEquals(0, result.getRemovedAttachments().size());
        verify(raddAltService, never()).isAreaCovered(any(), any(), any());
    }

    @Test
    void testApplyFilter_RemovesAllAttachments_ThrowsException() {
        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("ALTRO");
        List<PnAttachmentInfo> attachments = List.of(info1);

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.valueOf("LIGHT"));
        when(raddAltService.isAreaCovered(any(), any(), any())).thenReturn(Mono.just(true));

        Mono<PnDeliveryRequest> resultMono = service.filterAttachmentsToSend(req, attachments, address);
        assertThrows(PnInvalidChainRuleException.class, resultMono::block);
    }
}
