package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;

import java.util.*;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnCost fromCostDTO(String tenderCode, String driverCode, CostDTO dto){
        PnCost cost = new PnCost();
        cost.setTenderCode(tenderCode);
        cost.setDeliveryDriverCode(driverCode);
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

    public static CostDTO toCostDTO(PnCost paperCost){
        CostDTO dto = new CostDTO();
        dto.setCap(paperCost.getCap());
        dto.setPrice(paperCost.getBasePrice());
        dto.setPriceAdditional(paperCost.getPagePrice());
        dto.setProductType(ProductTypeEnumDto.fromValue(paperCost.getProductType()));
        return dto;
    }

    public static PageModel<PnCost> toPagination(Pageable pageable, List<PnCost> list){
        return PageModel.builder(list, pageable);
    }

    public static PageableCostResponseDto toPageableResponse(PageModel<PnCost> pnCostPageModel){
        PageableCostResponseDto response = new PageableCostResponseDto();
        response.setPageable(pnCostPageModel.getPageable());
        response.setNumber(pnCostPageModel.getNumber());
        response.setNumberOfElements(pnCostPageModel.getNumberOfElements());
        response.setSize(pnCostPageModel.getSize());
        response.setTotalElements(pnCostPageModel.getTotalElements());
        response.setTotalPages((long) pnCostPageModel.getTotalPages());
        response.setFirst(pnCostPageModel.isFirst());
        response.setLast(pnCostPageModel.isLast());
        response.setEmpty(pnCostPageModel.isEmpty());
        response.setContent(pnCostPageModel.mapTo(CostMapper::toCostDTO));
        return response;
    }

}
