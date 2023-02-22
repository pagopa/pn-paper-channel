package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
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


    public static PnDeliveryDriver toEntity(DeliveryDriverDTO dto){
        return mapperDeliveryDriverToDto.toEntity(dto);
    }

    public static Map<PnDeliveryDriver, List<PnCost>> toEntityFromExcel(DeliveriesData deliveriesData,
                                                                        String tenderCode){
        //Set<DeliveryAndCost> costSet = new HashSet<>(deliveriesData.getDeliveriesAndCosts());
        //log.info("COST SET SIZE : {}", costSet.size() );

        Map<PnDeliveryDriver, List<PnCost>> result = new  HashMap<PnDeliveryDriver, List<PnCost>>();

        deliveriesData.getDeliveriesAndCosts().forEach(deliveryAndCost -> {
            //Create single pnDeliveryDriver
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setUniqueCode(deliveryAndCost.getUniqueCode());
            driver.setDenomination(deliveryAndCost.getDenomination());
            driver.setBusinessName(deliveryAndCost.getBusinessName());
            driver.setRegisteredOffice(deliveryAndCost.getRegisteredOffice());
            driver.setPec(deliveryAndCost.getPec());
            driver.setFiscalCode(deliveryAndCost.getFiscalCode());
            driver.setTaxId(deliveryAndCost.getTaxId());
            driver.setPhoneNumber(deliveryAndCost.getPhoneNumber());
            driver.setUniqueCode(deliveryAndCost.getUniqueCode());
            driver.setFsu(deliveryAndCost.getFsu());

            //Create cost object
            PnCost pnCost = new PnCost();
            pnCost.setZone(deliveryAndCost.getZone());
            pnCost.setProductType(deliveryAndCost.getProductType());
            pnCost.setBasePrice(deliveryAndCost.getBasePrice());
            pnCost.setPagePrice(deliveryAndCost.getPagePrice());
            List <String> capList = deliveryAndCost.getCaps();
            pnCost.setCap(capList);

            if( result.containsKey(driver)){
                List <PnCost> costs = result.get(driver);
                costs.add(pnCost);
                //Check unique ProductType
                checkUniqueProductType( costs);
                result.put(driver, costs);
            }
            else{
                List <PnCost> costs = new ArrayList<PnCost>();
                costs.add(pnCost);
                //Check unique ProductType
                checkUniqueProductType( costs);
                result.put(driver, costs);
            }

        });

        //Check unique taxId
        Set<PnDeliveryDriver> driver = new HashSet<>(result.keySet());
        checkUniqueTaxId(driver);

        return result;

    }

    public static void  checkUniqueTaxId(Set<PnDeliveryDriver> driver ) throws PnExcelValidatorException {
       Set<String> taxIds = new HashSet<String>();
        driver.forEach(elem ->{
            taxIds.add(elem.getTaxId());
        });
        if (taxIds.size()!=driver.size()) throw new PnExcelValidatorException(ExceptionTypeEnum.DATA_NULL_OR_INVALID, null);
    }
    public static void   checkUniqueProductType(List<PnCost> cost ) throws PnExcelValidatorException {
        Set<String> productType = new HashSet<String>();
        cost.forEach(elem ->{
            productType.add(elem.getProductType());
        });
        if (productType.size()!=cost.size()) throw new PnExcelValidatorException(ExceptionTypeEnum.DATA_NULL_OR_INVALID, null);
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
}
