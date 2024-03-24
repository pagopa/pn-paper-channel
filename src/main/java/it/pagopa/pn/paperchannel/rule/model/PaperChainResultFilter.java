package it.pagopa.pn.paperchannel.rule.model;

import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class PaperChainResultFilter extends ListChainResultFilter<AttachmentInfo> {

    private String resultCode;
    private String resultDiagnostic;

}
