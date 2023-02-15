package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnCost fromCostDTO(String tenderCode, String driverCode, CostDTO dto){
        PnCost cost = new PnCost();
        cost.setTenderCode(tenderCode);
        cost.setIdDeliveryDriver(driverCode);
        cost.setUuid(dto.getCode());
        if (StringUtils.isBlank(cost.getUuid())){
            cost.setUuid(UUID.randomUUID().toString());
        }
        cost.setBasePrice(dto.getPrice());
        cost.setPagePrice(dto.getPriceAdditional());
        cost.setProductType(dto.getProductType().getValue());
        cost.setCap(dto.getCap());
        if (dto.getZone() != null ) {
            cost.setZone(dto.getZone().getValue());
        }
        return cost;
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


    public static CostDTO toCostDTO(PnCost paperCost){
        CostDTO dto = new CostDTO();
        dto.setCap(paperCost.getCap());
        dto.setPrice(paperCost.getBasePrice());
        dto.setPriceAdditional(paperCost.getPagePrice());
        dto.setProductType(ProductTypeEnumDto.fromValue(paperCost.productType));
        return dto;
    }

    public static List<PnCost> toEntity(String tenderCode, String uniqueCode, CostDTO request) {
        if (request.getZone() != null ){
            return Collections.singletonList(fromCostDTO(tenderCode, uniqueCode, request));
        }
        List<String> caps = Arrays.stream(request.getCap().split(",")).toList();
        return caps.stream()
                .map(cap ->{
                    request.setCap(cap);
                    return fromCostDTO(tenderCode, uniqueCode, request);
                }).toList();
    }
}
