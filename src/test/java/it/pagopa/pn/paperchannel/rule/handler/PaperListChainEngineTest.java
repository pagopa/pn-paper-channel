package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.commons.rules.ListChainEngineHandler;
import it.pagopa.pn.commons.rules.ListChainHandler;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsRule;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.impl.AttachmentsConfigServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaperListChainEngineTest {

    @Mock
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
}