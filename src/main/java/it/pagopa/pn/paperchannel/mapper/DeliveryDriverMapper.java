package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;

public class DeliveryDriverMapper {
    public static PnPaperDeliveryDriver toContractRequest(ContractInsertRequestDto contractInsertRequestDto){
        PnPaperDeliveryDriver contractRequest = new PnPaperDeliveryDriver();
        contractRequest.setUniqueCode(contractInsertRequestDto.getUniqueCode());
        contractRequest.setDenomination(contractInsertRequestDto.getDenomination());
        contractRequest.setTaxId(contractInsertRequestDto.getTaxId());
        contractRequest.setPhoneNumber(contractInsertRequestDto.getPhoneNumber());
        contractRequest.setFsu(contractInsertRequestDto.getFsu());
        return contractRequest;
    }
}
