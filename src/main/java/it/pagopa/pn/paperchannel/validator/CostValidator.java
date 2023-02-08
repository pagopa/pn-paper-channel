package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.utils.Const;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CostValidator {
    private CostValidator() {
        throw new IllegalCallerException();
    }

    public static void validateCosts(List<String> listFromDb, List<String> listFromRequest){
        Set<String> costSet = new HashSet<>(listFromDb);
        costSet.addAll(listFromRequest);
        if (costSet.size() < (listFromDb.size() + listFromRequest.size())){
            throw new PnGenericException(ExceptionTypeEnum.COST_ALREADY_EXIST, ExceptionTypeEnum.COST_ALREADY_EXIST.getMessage());
        }
    }
}
