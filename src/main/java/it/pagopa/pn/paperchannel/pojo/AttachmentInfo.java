package it.pagopa.pn.paperchannel.pojo;

import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class AttachmentInfo {
    private String id;
    private String documentType;
    private String url;
    private String date;
    private String fileKey;
    private int numberOfPage;
}
