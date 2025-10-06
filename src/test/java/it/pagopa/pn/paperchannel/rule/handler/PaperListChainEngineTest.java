package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.commons.rules.ListChainEngineHandler;
import it.pagopa.pn.commons.rules.ListChainHandler;
import it.pagopa.pn.commons.rules.model.ListFilterChainResult;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsRule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRuleParams;
import it.pagopa.pn.paperchannel.service.impl.AttachmentsConfigServiceImpl;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaperListChainEngineTest {

    @Spy
    private ListChainEngineHandler<PnAttachmentInfo, PnDeliveryRequest> listChainEngineHandler;


    @InjectMocks
    private PaperListChainEngine paperListChainEngine;
    @Test
    void resolveHandlerFromRule() {

        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);

        ListChainHandler<PnAttachmentInfo, PnDeliveryRequest> handler = paperListChainEngine.resolveHandlerFromRule(rule);
        Assertions.assertTrue(handler instanceof DocumentTagHandler);
    }

    @Test
    void resolveHandlerFromRule_exception() {

        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType("wrong");

        Assertions.assertThrows(PnInvalidChainRuleException.class, ()-> paperListChainEngine.resolveHandlerFromRule(rule));


    }

    @Test
    void idempotencyOkThanksToGetAllAttachmentsMethodTest() {
        PnRuleParams params = new PnRuleParams();
        params.setTypeWithSuccessResult("AAR");
        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);
        rule.setParams(params);

        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();

        PnAttachmentInfo aar = new PnAttachmentInfo();
        aar.setFileKey("safestorage://filekey1?docTag=AAR");
        aar.setDocTag("AAR");

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("safestorage://filekey2?docTag=DOCUMENT");
        attachment.setDocTag("DOCUMENT");

        pnDeliveryRequest.setAttachments(new ArrayList<>(List.of(aar, attachment)));

        List<PnAttachmentInfo> allAttachments = AttachmentsConfigUtils.getAllAttachments(pnDeliveryRequest);
        List<ListFilterChainResult<PnAttachmentInfo>> firstResult = paperListChainEngine.filterItems(pnDeliveryRequest, allAttachments, List.of(rule)).collectList().block();

        pnDeliveryRequest = AttachmentsConfigServiceImpl.sendFilteredAttachments(pnDeliveryRequest, firstResult);

        assertThat(pnDeliveryRequest.getAttachments()).hasSize(1).containsExactly(aar);
        assertThat(pnDeliveryRequest.getRemovedAttachments()).hasSize(1).containsExactly(attachment);

        // secondo filtraggio
        allAttachments = AttachmentsConfigUtils.getAllAttachments(pnDeliveryRequest);
        List<ListFilterChainResult<PnAttachmentInfo>> secondResult = paperListChainEngine.filterItems(pnDeliveryRequest, allAttachments, List.of(rule)).collectList().block();

        assertThat(secondResult).isEqualTo(firstResult);

        pnDeliveryRequest = AttachmentsConfigServiceImpl.sendFilteredAttachments(pnDeliveryRequest, secondResult);
        assertThat(pnDeliveryRequest.getAttachments()).hasSize(1).containsExactly(aar);
        assertThat(pnDeliveryRequest.getRemovedAttachments()).hasSize(1).containsExactly(attachment);
    }

    @Test
    void idempotencyKoWithoutGetAllAttachmentsMethodTest() {
        PnRuleParams params = new PnRuleParams();
        params.setTypeWithSuccessResult("AAR");
        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);
        rule.setParams(params);

        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();

        PnAttachmentInfo aar = new PnAttachmentInfo();
        aar.setFileKey("safestorage://filekey1?docTag=AAR");
        aar.setDocTag("AAR");

        PnAttachmentInfo attachment = new PnAttachmentInfo();
        attachment.setFileKey("safestorage://filekey2?docTag=DOCUMENT");
        attachment.setDocTag("DOCUMENT");

        pnDeliveryRequest.setAttachments(new ArrayList<>(List.of(aar, attachment)));

        List<ListFilterChainResult<PnAttachmentInfo>> firstResult = paperListChainEngine.filterItems(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), List.of(rule)).collectList().block();
        assertThat(firstResult).isNotNull();
        pnDeliveryRequest = AttachmentsConfigServiceImpl.sendFilteredAttachments(pnDeliveryRequest, firstResult);

        assertThat(pnDeliveryRequest.getAttachments()).hasSize(1).containsExactly(aar);
        assertThat(pnDeliveryRequest.getRemovedAttachments()).hasSize(1).containsExactly(attachment);

        // secondo filtraggio
        List<ListFilterChainResult<PnAttachmentInfo>> secondResult = paperListChainEngine.filterItems(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), List.of(rule)).collectList().block();

        assertThat(secondResult).isNotEqualTo(firstResult);

        pnDeliveryRequest = AttachmentsConfigServiceImpl.sendFilteredAttachments(pnDeliveryRequest, secondResult);
        assertThat(pnDeliveryRequest.getAttachments()).hasSize(1).containsExactly(aar);
        assertThat(pnDeliveryRequest.getRemovedAttachments()).isEmpty();
    }

}