package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnCost fromContractDTO(ContractDto contractDto){

        PnCost costs = new PnCost();
        costs.setBasePrice(contractDto.getPrice());
        costs.setPagePrice(contractDto.getPriceAdditional());
        costs.setProductType(contractDto.getRegisteredLetter().getValue());
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
                    pricesDto.getNationals().add(toNationalContract(cost));
                }
                else if (StringUtils.isNotBlank(cost.getZone())) {
                    pricesDto.getInternationals().add(toInternationalContract(cost));
                }
            });
        }
        dto.setData(pricesDto);
        return dto;
    }


    public static NationalContractDto toNationalContract(PnCost paperCost){
        NationalContractDto dto = new NationalContractDto();
        dto.setCap(paperCost.getCap());
        dto.setPrice(BigDecimal.valueOf(paperCost.getBasePrice()));
        dto.setPriceAdditional(BigDecimal.valueOf(paperCost.getPagePrice()));
        dto.setRegisteredLetter(TypeRegisteredLetterEnum.fromValue(paperCost.productType));
        return dto;
    }

    public static InternationalContractDto toInternationalContract(PnCost paperCost){
        InternationalContractDto dto = new InternationalContractDto();
        dto.setZone(InternationalZoneEnum.fromValue(paperCost.getZone()));
        dto.setPrice(BigDecimal.valueOf(paperCost.getBasePrice()));
        dto.setPriceAdditional(BigDecimal.valueOf(paperCost.getPagePrice()));
        dto.setRegisteredLetter(TypeRegisteredLetterInterEnum.fromValue(paperCost.productType));
        return dto;
    }
}
