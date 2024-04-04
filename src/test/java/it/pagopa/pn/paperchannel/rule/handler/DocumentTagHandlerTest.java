package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.commons.rules.model.FilterHandlerResult;
import it.pagopa.pn.commons.rules.model.FilterHandlerResultEnum;
import it.pagopa.pn.commons.rules.model.ListFilterChainResult;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRuleParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTagHandlerTest {

    DocumentTagHandler documentTagHandler;

    @BeforeEach
    public void init(){
        PnRuleParams params = new PnRuleParams();
        params.setTypeWithSuccessResult("AAR");
        params.setTypeWithNextResult("DOCUMENT,F24");
        documentTagHandler = new DocumentTagHandler(params);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "AAR, SUCCESS",
            "DOCUMENT, NEXT",
            "F24, NEXT",
            "PIPPO, FAIL",
    })
    @ExtendWith(MockitoExtension.class)
    void filter(String docTag, String expectedResult) {
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDocTag(docTag);

        Mono<FilterHandlerResult> mono = documentTagHandler.filter(pnAttachmentInfo, null);
        FilterHandlerResult r        = mono.block();

        Assertions.assertNotNull(r);
        Assertions.assertEquals(expectedResult, r.getResult().name());
    }
}