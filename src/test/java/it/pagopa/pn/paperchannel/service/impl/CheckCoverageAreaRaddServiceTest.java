package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.RaddCoverageReferenceDate;
import it.pagopa.pn.paperchannel.model.RaddSearchMode;
import it.pagopa.pn.paperchannel.service.RaddAltService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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

    @ParameterizedTest
    @EnumSource(RaddCoverageReferenceDate.class)
    void testFilterAttachmentsToSend_AreaCovered_FiltersCorrectly(RaddCoverageReferenceDate referenceDate) {
        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.LIGHT);
        when(cfg.getRaddCoverageReferenceDate()).thenReturn(referenceDate);
        when(raddAltService.isAreaCovered(any(), any(), any()))
                .thenReturn(Mono.just(true));

        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);
        req.setNotificationSentAt(Instant.parse("2024-01-20T10:00:00Z"));
        req.setStartDate("2024-01-22T12:00:00Z");

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("AAR");
        PnAttachmentInfo info2 = new PnAttachmentInfo();
        info2.setDocTag("ALTRO");

        List<PnAttachmentInfo> attachments = Arrays.asList(info1, info2);

        PnDeliveryRequest result = service.filterAttachmentsToSend(req, attachments, address).block();

        assertNotNull(result);
        assertSame(req, result);
        assertEquals(1, result.getAttachments().size());
        assertEquals("AAR", result.getAttachments().getFirst().getDocTag());
        assertEquals(1, result.getRemovedAttachments().size());
        assertEquals("ALTRO", result.getRemovedAttachments().getFirst().getDocTag());
        assertEquals(referenceDate.name(), result.getRaddCoverageReferenceDate());

        Instant expectedDate =
                referenceDate == RaddCoverageReferenceDate.PREPARE_REQUEST_DATE
                        ? Instant.parse(req.getStartDate())
                        : req.getNotificationSentAt();

        verify(raddAltService)
                .isAreaCovered(RaddSearchMode.LIGHT, address, expectedDate);
    }

    @Test
    void testFilterAttachmentsToSend_WhenStartDateIsInvalid() {
        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.LIGHT);
        when(cfg.getRaddCoverageReferenceDate()).thenReturn(RaddCoverageReferenceDate.PREPARE_REQUEST_DATE);
        when(raddAltService.isAreaCovered(any(), any(), isNull())).thenReturn(Mono.just(true));

        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);
        req.setNotificationSentAt(Instant.parse("2024-01-20T10:00:00Z"));
        req.setStartDate("invalid-date");

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info = new PnAttachmentInfo();
        info.setDocTag("AAR");

        assertThrows(DateTimeParseException.class,
                () -> service.filterAttachmentsToSend(req, List.of(info), address).block());

        assertNull(req.getRaddCoverageReferenceDate());
        verifyNoInteractions(raddAltService);
    }

    @ParameterizedTest
    @EnumSource(RaddCoverageReferenceDate.class)
    void testFilterAttachmentsToSend_AreaNotCovered_SendsAll(RaddCoverageReferenceDate referenceDate) {
        when(cfg.getRaddCoverageSearchMode()).thenReturn(RaddSearchMode.COMPLETE);
        when(cfg.getRaddCoverageReferenceDate()).thenReturn(referenceDate);
        when(raddAltService.isAreaCovered(any(), any(), any())).thenReturn(Mono.just(false));

        PnDeliveryRequest req = new PnDeliveryRequest();
        req.setAarWithRadd(true);
        req.setNotificationSentAt(Instant.parse("2024-01-20T10:00:00Z"));
        req.setStartDate("2024-01-22T12:00:00Z");

        PnAddress address = new PnAddress();
        address.setCountry("ITALIA");

        PnAttachmentInfo info1 = new PnAttachmentInfo();
        info1.setDocTag("AAR");
        PnAttachmentInfo info2 = new PnAttachmentInfo();
        info2.setDocTag("ALTRO");

        List<PnAttachmentInfo> attachments = Arrays.asList(info1, info2);

        PnDeliveryRequest result = service.filterAttachmentsToSend(req, attachments, address).block();

        assertNotNull(result);
        assertSame(req, result);
        assertEquals(attachments, result.getAttachments());
        assertTrue(result.getRemovedAttachments().isEmpty());
        assertEquals(referenceDate.name(), result.getRaddCoverageReferenceDate());

        Instant expectedDate =
                referenceDate == RaddCoverageReferenceDate.PREPARE_REQUEST_DATE
                        ? Instant.parse(req.getStartDate())
                        : req.getNotificationSentAt();

        verify(raddAltService)
                .isAreaCovered(RaddSearchMode.COMPLETE, address, expectedDate);
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
