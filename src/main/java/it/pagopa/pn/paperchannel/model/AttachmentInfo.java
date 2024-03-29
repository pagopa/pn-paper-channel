package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.utils.Const;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
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
    private String generatedFrom;
    private String docTag;

    @Override
    public int compareTo(@NotNull AttachmentInfo attachmentInfo) {
        boolean isThisAAR = StringUtils.equalsIgnoreCase(this.documentType, Const.PN_AAR);
        boolean isEquals = StringUtils.equals(this.getDocumentType(), attachmentInfo.getDocumentType());
        if (isThisAAR) {
            if (isEquals) return 0;
            return -1;
        }
        if (isEquals) return 0;
        return 1;
    }
}
