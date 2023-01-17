package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractDto;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnPaperCost fromContractDTO(ContractDto contractDto){

        PnPaperCost costs = new PnPaperCost();
        costs.setBasePrice(contractDto.getPrice());
        costs.setPagePrice(contractDto.getPriceAdditional());
        costs.setStartDate(DateUtils.formatDate(new Date()));
        costs.setEndDate(DateUtils.formatDate(new Date()));
        costs.setProductType(contractDto.getRegisteredLetter().getValue());
        costs.setCap(contractDto.getCap());
        if (contractDto.getZone() != null ) {
            costs.setZone(contractDto.getZone().getValue());
        }


        return costs;
    }
}
