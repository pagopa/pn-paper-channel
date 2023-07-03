package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InternationalZoneEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PageableCostResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnumDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.model.PageModel;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;

import java.util.*;


public class CostMapper {

    private CostMapper(){
        throw new IllegalCallerException();
    }

    public static PnCost fromCostDTO(String tenderCode, String taxId, CostDTO dto){
        PnCost cost = new PnCost();
        cost.setTenderCode(tenderCode);
        cost.setDeliveryDriverCode(taxId);
        cost.setUuid(dto.getUid());
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
        dto.setUid(paperCost.getUuid());
        dto.setTenderCode(paperCost.getTenderCode());
        dto.setDriverCode(paperCost.getDeliveryDriverCode());
        dto.setCap(paperCost.getCap());
        if (StringUtils.isNotBlank(paperCost.getZone())){
            dto.setZone(InternationalZoneEnum.fromValue(paperCost.getZone()));
        }
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
