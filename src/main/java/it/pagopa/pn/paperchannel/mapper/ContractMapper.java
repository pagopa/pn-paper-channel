package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractDto;

public class ContractMapper {

    private ContractMapper(){
        throw new IllegalCallerException();
    }

    public static Contract toContract(PnPaperCost costs){
        Contract contract = new Contract();
        contract.setPrice(costs.getBasePrice().doubleValue());
        contract.setPricePerPage(costs.getPagePrice().doubleValue());
        return contract;
    }
}
