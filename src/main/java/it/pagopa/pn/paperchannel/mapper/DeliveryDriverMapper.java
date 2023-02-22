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
import it.pagopa.pn.paperchannel.utils.costutils.CapProductType;
import it.pagopa.pn.paperchannel.utils.costutils.ZoneProductType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
            pnCost.setFsu(deliveryAndCost.getFsu());

            if( result.containsKey(driver)){
                List <PnCost> costs = result.get(driver);
                costs.add(pnCost);
                result.put(driver, costs);
            }
            else{
                List <PnCost> costs = new ArrayList<PnCost>();
                costs.add(pnCost);
                result.put(driver, costs);
            }

        });

        //Check Unique Product Type
        checkUniqueCapProductType(result);

        //Check Unique Zone Product Type
        checkUniqueZoneProductType(result);
        return result;

    }


    public static void   checkUniqueCapProductType(Map<PnDeliveryDriver,List<PnCost> > result ) throws PnExcelValidatorException {
        //Create list with single element cap - product type
        List<CapProductType> costList = new ArrayList<CapProductType>();
        result.forEach( (k,v) ->{
            //Get List of costs
            List<PnCost> singleCostList = result.get(k);
            singleCostList.forEach(elem ->{
                //Get List of caps
                List <String> caps = elem.getCap() ;
                if (caps!=null) {
                    CapProductType capProductType = new CapProductType();
                    capProductType.setProductType(elem.getProductType());
                    caps.forEach(singleCap -> {
                        //Add into cost list every single couple cap - product type
                        capProductType.setCap(singleCap);
                        costList.add(capProductType);
                    });
                }

            });
        });
        Set setCostList = new HashSet(costList);
        if ( !(costList.size()== setCostList.size())) throw new PnExcelValidatorException(ExceptionTypeEnum.DATA_NULL_OR_INVALID, null);
    }

    public static void   checkUniqueZoneProductType(Map<PnDeliveryDriver,List<PnCost> > result ) throws PnExcelValidatorException {
        //Create list with single element cap - product type
        Map<ZoneProductType, List<Boolean> > costList = new HashMap<ZoneProductType, List<Boolean>>();
        result.forEach( (k,v) ->{
            //Get List of costs
            List<PnCost> singleCostList = result.get(k);
            singleCostList.forEach(elem ->{
                if (elem.getZone()!=null){
                        //key
                        ZoneProductType zoneProductType = new ZoneProductType();
                        zoneProductType.setProductType(elem.getProductType());
                        zoneProductType.setZone(elem.getZone()) ;
                        //value
                        Boolean fsu = elem.getFsu();

                        //insert
                        if (costList.containsKey(zoneProductType)){
                            List<Boolean> values= costList.get(zoneProductType);
                            values.add(elem.getFsu());
                            costList.put(zoneProductType, values);
                        }
                        else{
                            List<Boolean> values= new ArrayList<Boolean>();
                            values.add(elem.getFsu());
                            costList.put(zoneProductType, values);
                        }

                }
            });
        });
        costList.forEach((k,v) ->{
            if ( v.size()>2
                    || (v.size()==1 &&v.get(0)==false)
                    || (v.size()==2 && (v.get(0)==true && v.get(1)==true))
                    || (v.size()==2 &&(v.get(0)==false && v.get(1)==false))
            )
                    throw new PnExcelValidatorException(ExceptionTypeEnum.DATA_NULL_OR_INVALID, null);


        });
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
