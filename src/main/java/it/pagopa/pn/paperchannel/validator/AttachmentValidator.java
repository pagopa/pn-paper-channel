package it.pagopa.pn.paperchannel.validator;

import java.util.Comparator;
import java.util.List;

public class AttachmentValidator {
    private AttachmentValidator() {
        throw new IllegalCallerException();
    }

    public static boolean checkBetweenLists(List<String> first, List<String> second){
        if (first.size() != second.size()) {
            return false;
        }

        first.sort(Comparator.naturalOrder());
        second.sort(Comparator.naturalOrder());

        for (int i = 0; i < first.size() ; i++){
            if (!first.get(i).equals(second.get(i))){
                return false;
            }
        }
        return true;
    }
}
