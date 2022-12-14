package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class AttachmentInfo {
    private String id;
    private String documentType;
    private String url;
    private String date;
    private int numberOfPage;
}
