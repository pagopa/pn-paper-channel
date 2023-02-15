package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.model.Contract;

public class ContractMapper {

    private ContractMapper(){
        throw new IllegalCallerException();
    }

    public static Contract toContract(PnCost costs){
        Contract contract = new Contract();
        contract.setPrice(costs.getBasePrice().doubleValue());
        contract.setPricePerPage(costs.getPagePrice().doubleValue());
        return contract;
    }
}
