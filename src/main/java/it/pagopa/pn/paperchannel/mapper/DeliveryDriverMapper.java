package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.*;

public class DeliveryDriverMapper {

    private DeliveryDriverMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnPaperDeliveryDriver, DeliveryDriverDto> mapperDeliveryDriverToDto = new BaseMapperImpl<>(PnPaperDeliveryDriver.class, DeliveryDriverDto.class);
    private static final BaseMapper<PnPaperDeliveryDriver, DeliveryAndCost> mapperDeliveryCost = new BaseMapperImpl<>(PnPaperDeliveryDriver.class, DeliveryAndCost.class);
    private static final BaseMapper<PnPaperCost, DeliveryAndCost> mapperCost = new BaseMapperImpl<>(PnPaperCost.class, DeliveryAndCost.class);

    /*
    public static PnPaperDeliveryDriver toContractRequest(ContractInsertRequestDto contractInsertRequestDto){
        PnPaperDeliveryDriver contractRequest = new PnPaperDeliveryDriver();
        contractRequest.setUniqueCode(contractInsertRequestDto.getUniqueCode());
        contractRequest.setDenomination(contractInsertRequestDto.getDenomination());
        contractRequest.setTaxId(contractInsertRequestDto.getTaxId());
        contractRequest.setPhoneNumber(contractInsertRequestDto.getPhoneNumber());
        contractRequest.setFsu(contractInsertRequestDto.getFsu());
        return contractRequest;
    }
*/
    public static Map<PnPaperDeliveryDriver, List<PnPaperCost>> toEntityFromExcel(DeliveriesData deliveriesData, String tenderCode){
        Map<PnPaperDeliveryDriver, List<PnPaperCost>> map = new HashMap<>();
        deliveriesData.getDeliveriesAndCosts().forEach(deliveryAndCost -> {
            PnPaperDeliveryDriver driver = new PnPaperDeliveryDriver();
            driver.setUniqueCode(deliveryAndCost.getUniqueCode());
            if(!map.containsKey(driver)){
                driver = mapperDeliveryCost.toEntity(deliveryAndCost);
                map.put(driver, new ArrayList<>());
            }
            List<PnPaperCost> costList = map.get(driver);
            PnPaperCost cost = mapperCost.toEntity(deliveryAndCost);
            cost.setIdDeliveryDriver(driver.getUniqueCode());
            cost.setUuid(UUID.randomUUID().toString());
            cost.setTenderCode(tenderCode);
            costList.add(cost);
        });
        return map;
    }
    public static PageableDeliveryDriverResponseDto toPageableResponse(PageModel<PnPaperDeliveryDriver> pagePnPaperDeliveryDriver) {
        PageableDeliveryDriverResponseDto pageableDeliveryDriverResponseDto = new PageableDeliveryDriverResponseDto();
        pageableDeliveryDriverResponseDto.setPageable(pagePnPaperDeliveryDriver.getPageable());
        pageableDeliveryDriverResponseDto.setNumber(pagePnPaperDeliveryDriver.getNumber());
        pageableDeliveryDriverResponseDto.setNumberOfElements(pagePnPaperDeliveryDriver.getNumberOfElements());
        pageableDeliveryDriverResponseDto.setSize(pagePnPaperDeliveryDriver.getSize());
        pageableDeliveryDriverResponseDto.setTotalElements(pagePnPaperDeliveryDriver.getTotalElements());
        pageableDeliveryDriverResponseDto.setTotalPages((long) pagePnPaperDeliveryDriver.getTotalPages());
        pageableDeliveryDriverResponseDto.setFirst(pagePnPaperDeliveryDriver.isFirst());
        pageableDeliveryDriverResponseDto.setLast(pagePnPaperDeliveryDriver.isLast());
        pageableDeliveryDriverResponseDto.setEmpty(pagePnPaperDeliveryDriver.isEmpty());
        pageableDeliveryDriverResponseDto.setContent(pagePnPaperDeliveryDriver.mapTo(DeliveryDriverMapper::deliveryDriverToDto));
        return pageableDeliveryDriverResponseDto;
    }

    public static DeliveryDriverDto deliveryDriverToDto(PnPaperDeliveryDriver pnPaperDeliveryDriver) {
        return mapperDeliveryDriverToDto.toDTO(pnPaperDeliveryDriver);
    }

    public static PageModel<PnPaperDeliveryDriver> toPagination(Pageable pageable, List<PnPaperDeliveryDriver> list){
        return PageModel.builder(list, pageable);
    }

}
