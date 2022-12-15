package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class AttachmentInfo {
    private String id;
    private String documentType;
    private String url;
    private String date;
    private String fileKey;
    private int numberOfPage;
}
