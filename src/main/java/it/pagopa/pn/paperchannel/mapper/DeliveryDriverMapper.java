package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.*;

public class DeliveryDriverMapper {

    private DeliveryDriverMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnDeliveryDriver, DeliveryDriverDto> mapperDeliveryDriverToDto = new BaseMapperImpl<>(PnDeliveryDriver.class, DeliveryDriverDto.class);
    private static final BaseMapper<PnDeliveryDriver, DeliveryAndCost> mapperDeliveryCost = new BaseMapperImpl<>(PnDeliveryDriver.class, DeliveryAndCost.class);
    private static final BaseMapper<PnCost, DeliveryAndCost> mapperCost = new BaseMapperImpl<>(PnCost.class, DeliveryAndCost.class);

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
    public static Map<PnDeliveryDriver, List<PnCost>> toEntityFromExcel(DeliveriesData deliveriesData, String tenderCode){
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        deliveriesData.getDeliveriesAndCosts().forEach(deliveryAndCost -> {
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setUniqueCode(deliveryAndCost.getUniqueCode());
            if(!map.containsKey(driver)){
                driver = mapperDeliveryCost.toEntity(deliveryAndCost);
                map.put(driver, new ArrayList<>());
            }
            List<PnCost> costList = map.get(driver);
            PnCost cost = mapperCost.toEntity(deliveryAndCost);
            cost.setIdDeliveryDriver(driver.getUniqueCode());
            cost.setUuid(UUID.randomUUID().toString());
            cost.setTenderCode(tenderCode);
            costList.add(cost);
        });
        return map;
    }
    public static PageableDeliveryDriverResponseDto toPageableResponse(PageModel<PnDeliveryDriver> pagePnPaperDeliveryDriver) {
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

    public static DeliveryDriverDto deliveryDriverToDto(PnDeliveryDriver pnDeliveryDriver) {
        return mapperDeliveryDriverToDto.toDTO(pnDeliveryDriver);
    }

    public static PageModel<PnDeliveryDriver> toPagination(Pageable pageable, List<PnDeliveryDriver> list){
        return PageModel.builder(list, pageable);
    }

}
