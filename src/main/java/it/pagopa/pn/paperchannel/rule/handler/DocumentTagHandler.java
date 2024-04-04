package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.commons.rules.model.FilterHandlerResult;
import it.pagopa.pn.commons.rules.model.FilterHandlerResultEnum;
import it.pagopa.pn.commons.rules.model.ListChainContext;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRuleParams;
import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.core.publisher.Mono;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
public class DocumentTagHandler extends it.pagopa.pn.commons.rules.ListChainHandler<PnAttachmentInfo, PnDeliveryRequest> {

    public static final String RULE_TYPE = "paper-doc-tag-handler";

    private List<String> typeWithNextResult;
    private List<String> typeWithSuccessResult;

    public DocumentTagHandler(PnRuleParams params){
        typeWithNextResult = List.of(params.getTypeWithNextResult().split(","));
        typeWithSuccessResult = List.of(params.getTypeWithSuccessResult().split(","));
    }

    @Override
    public Mono<FilterHandlerResult> filter(PnAttachmentInfo item, ListChainContext<PnAttachmentInfo, PnDeliveryRequest> ruleContext) {

        if (typeWithSuccessResult.contains(item.getDocTag()))
            return Mono.just(new FilterHandlerResult(FilterHandlerResultEnum.SUCCESS, "DOC_TAG_REQUIRED", "Il tag " + item.getDocTag() + " rientra tra quelli da inviare obbligatoriamente"));
        if (typeWithNextResult.contains(item.getDocTag()))
            return Mono.just(new FilterHandlerResult(FilterHandlerResultEnum.NEXT, "DOC_TAG_ACCEPTED", "Il tag " + item.getDocTag() + " rientra tra quelli da inviare"));


        return Mono.just(new FilterHandlerResult(FilterHandlerResultEnum.FAIL, "DOC_TAG_SKIPPED", "Il tag " + item.getDocTag() + " non rientra tra quelli da inviare"));
    }
}
