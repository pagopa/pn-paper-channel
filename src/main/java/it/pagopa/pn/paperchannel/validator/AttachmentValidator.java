package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;

import java.util.Comparator;
import java.util.List;

public class AttachmentValidator {
    private AttachmentValidator() {
        throw new IllegalCallerException();
    }

    public static boolean checkBetweenLists(List<String> attachmentUrls, List<PnAttachmentInfo> attachments){
        attachmentUrls.sort(Comparator.naturalOrder());
        attachments.sort(Comparator.comparing(PnAttachmentInfo::getFileKey));
        if (attachmentUrls.size() != attachments.size()) {
            return false;
        }
        else {
            for (int i = 0; i < attachmentUrls.size() ; i++){
                if (!attachmentUrls.get(i).equals(attachments.get(i).getFileKey())){
                    return false;
                }
            }
            return true;
        }
    }
}
