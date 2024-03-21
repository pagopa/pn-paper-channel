package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.rule.model.*;

import java.util.List;

public class DocumentTypeRuleHandler implements RuleHandler<DocumentTypeRuleModel> {

    @Override
    public ResultFilter evaluate(DocumentTypeRuleModel ruleModel, RuleAttachmentInfo currentAttachment, RuleContext ruleContext, List<RuleAttachmentInfo> filteredAttachments) {
        if(ruleModel.getTypeWithSuccesstResult().contains(currentAttachment.getDocumentType())) {
            return ResultFilter.builder()
                    .result(ResultFilter.ResultFilterEnum.SUCCESS)
                    .fileKey(currentAttachment.getFileKey())
                    .reasonCode("")
                    .reasonDescription("")
                    .build();
        }
        else if(ruleModel.getTypeWithNextResult().contains(currentAttachment.getDocumentType())) {
            return ResultFilter.builder()
                    .result(ResultFilter.ResultFilterEnum.NEXT)
                    .fileKey(currentAttachment.getFileKey())
                    .reasonCode("")
                    .reasonDescription("")
                    .build();
        }
        else {
            return ResultFilter.builder()
                    .result(ResultFilter.ResultFilterEnum.DISCARD)
                    .fileKey(currentAttachment.getFileKey())
                    .reasonCode("")
                    .reasonDescription("")
                    .build();
        }
    }
}
