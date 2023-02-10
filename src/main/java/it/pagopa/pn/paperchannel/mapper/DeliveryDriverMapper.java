package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import java.util.*;

@Slf4j
public class DeliveryDriverMapper {

    private DeliveryDriverMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnDeliveryDriver, DeliveryDriverDTO> mapperDeliveryDriverToDto = new BaseMapperImpl<>(PnDeliveryDriver.class, DeliveryDriverDTO.class);
    private static final BaseMapper<PnDeliveryDriver, DeliveryAndCost> mapperDeliveryCost = new BaseMapperImpl<>(PnDeliveryDriver.class, DeliveryAndCost.class);
    private static final BaseMapper<PnCost, DeliveryAndCost> mapperCost = new BaseMapperImpl<>(PnCost.class, DeliveryAndCost.class);


    public static PnDeliveryDriver toEntity(DeliveryDriverDTO dto){
        return mapperDeliveryDriverToDto.toEntity(dto);
    }

    public static Map<PnDeliveryDriver, List<PnCost>> toEntityFromExcel(DeliveriesData deliveriesData, String tenderCode){
        Set<DeliveryAndCost> costSet = new HashSet<>(deliveriesData.getDeliveriesAndCosts());
        log.info("COST SET SIZE : {}", costSet.size() );
        Map<PnDeliveryDriver, List<PnCost>> map = new HashMap<>();
        return map;
        /*
        Map<PnDeliveryDriver, Map<ProductTypeEnum, PnCost>> map = new HashMap<>();
        deliveriesData.getDeliveriesAndCosts().forEach(deliveryAndCost -> {
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setUniqueCode(deliveryAndCost.getUniqueCode());
            if(!map.containsKey(driver)){
                driver = mapperDeliveryCost.toEntity(deliveryAndCost);
                driver.setUniqueCode(deliveryAndCost.getUniqueCode());
                driver.setTenderCode(tenderCode);
                map.put(driver, new HashMap<>());
            }
            Map<ProductTypeEnum, PnCost> costMap = map.get(driver);
            ProductTypeEnum productType = getCorrectProductType(deliveryAndCost);

            if (costMap.containsKey(productType)){

            }


            if (StringUtils.isNotBlank(deliveryAndCost.getCap())){
                if (deliveryAndCost.getCap().contains(",")) {
                    List<String> capsWithRange = Arrays.stream(deliveryAndCost.getCap().split(",")).toList();
                    for (String cap : caps) {
                        if (cap.contains("-")) {
                            List<String> capRange = Arrays.stream(cap.split("-")).toList();
                            int first = Integer.parseInt(capRange.get(0));
                            int last = Integer.parseInt(capRange.get(1));
                            for (int i = first; i <= last; i++) {
                                PnCost newCost = getCost(driver, tenderCode, i+ "", deliveryAndCost);
                                costList.add(newCost);
                            }
                        } else {
                            PnCost newCost = getCost(driver, tenderCode, cap, deliveryAndCost);
                            costList.add(newCost);
                        }
                    }
                } else{
                    PnCost newCost = getCost(driver, tenderCode, deliveryAndCost.getCap(), deliveryAndCost);
                    costList.add(newCost);
                }
            }
            else{
                PnCost singleCost = getCost(driver, tenderCode, null, deliveryAndCost);
                costList.add(singleCost);
            }
        });
        return map;
         */
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

    public static DeliveryDriverDTO deliveryDriverToDto(PnDeliveryDriver pnDeliveryDriver) {
        return mapperDeliveryDriverToDto.toDTO(pnDeliveryDriver);
    }

    public static PageModel<PnDeliveryDriver> toPagination(Pageable pageable, List<PnDeliveryDriver> list){
        return PageModel.builder(list, pageable);
    }

    private static PnCost getCost(PnDeliveryDriver deliveryDriver, String tenderCode, String cap, DeliveryAndCost rowExcel){
        PnCost cost = mapperCost.toEntity(rowExcel);
        cost.setDeliveryDriverCode(deliveryDriver.getUniqueCode());
        cost.setUuid(UUID.randomUUID().toString());
        cost.setTenderCode(tenderCode);
        //cost.setCap(cap);
        return cost;
    }
}
