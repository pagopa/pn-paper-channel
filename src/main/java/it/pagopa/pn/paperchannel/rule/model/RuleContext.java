package it.pagopa.pn.paperchannel.rule.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class RuleContext {


    @ToString.Exclude
    private String fiscalCode;

    private String receiverType;

    private String iun;

    private String proposalProductType;

    private String printType;

    private String productType;

    private List<RuleAttachmentInfo> attachments;

    private String requestPaId;

    private RuleAddress address;

}
