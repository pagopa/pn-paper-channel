package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnCost fromContractDTO(CostDto contractDto){

        PnCost costs = new PnCost();
        costs.setBasePrice(contractDto.getPrice());
        costs.setPagePrice(contractDto.getPriceAdditional());
        costs.setProductType(contractDto.getProductType().getValue());
        costs.setCap(contractDto.getCap());
        if (contractDto.getZone() != null ) {
            costs.setZone(contractDto.getZone().getValue());
        }
        return costs;
    }

    public static AllPricesContractorResponseDto toResponse(List<PnCost> paperCosts){
        AllPricesContractorResponseDto dto = new AllPricesContractorResponseDto();
        BaseResponse baseResponse =  new BaseResponse();
        baseResponse.setResult(true);
        baseResponse.setCode(BaseResponse.CodeEnum.NUMBER_0);
        dto.setStatus(baseResponse);
        AllPricesDeliveryDriverDto pricesDto = new AllPricesDeliveryDriverDto();
        pricesDto.setInternationals(new ArrayList<>());
        pricesDto.setNationals(new ArrayList<>());
        if (paperCosts != null){
            paperCosts.forEach(cost -> {
                if (StringUtils.isNotBlank(cost.getCap())){
                    pricesDto.getNationals().add(toCostDTO(cost));
                }
                else if (StringUtils.isNotBlank(cost.getZone())) {
                    pricesDto.getInternationals().add(toCostDTO(cost));
                }
            });
        }
        dto.setData(pricesDto);
        return dto;
    }


    public static CostDto toCostDTO(PnCost paperCost){
        CostDto dto = new CostDto();
        dto.setCap(paperCost.getCap());
        dto.setPrice(paperCost.getBasePrice());
        dto.setPriceAdditional(paperCost.getPagePrice());
        dto.setProductType(ProductTypeEnumDto.fromValue(paperCost.productType));
        return dto;
    }
}
