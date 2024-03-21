package it.pagopa.pn.paperchannel.rule.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class RuleAttachmentInfo {
    private String documentType;
    private String date;
    private String fileKey;
    private Integer numberOfPage;
    private String documentTag;
}
