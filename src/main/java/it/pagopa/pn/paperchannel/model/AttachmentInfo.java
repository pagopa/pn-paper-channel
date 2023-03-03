package it.pagopa.pn.paperchannel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;


@Getter
@Setter
@ToString
public class AttachmentInfo implements Comparable<AttachmentInfo> {
    private String id;
    private String documentType;
    private String url;
    private String date;
    private String fileKey;
    private int numberOfPage;
    private String sha256;

    @Override
    public int compareTo(@NotNull AttachmentInfo attachmentInfo) {
        return this.fileKey.compareTo(attachmentInfo.fileKey);
    }
}
