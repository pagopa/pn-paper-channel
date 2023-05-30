package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnumDto;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;

import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.costutils.CapProductType;
import it.pagopa.pn.paperchannel.utils.costutils.ZoneProductType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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


        Map<PnDeliveryDriver, List<PnCost>> result = new  HashMap<>();
        List<CapProductType> nationalCosts = new ArrayList<>();
        List<ZoneProductType> internationalCosts = new ArrayList<>();


        deliveriesData.getDeliveriesAndCosts().forEach(deliveryAndCost -> {
            //Create single pnDeliveryDriver
            List<PnCost> costs = new ArrayList<>();
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setTaxId(deliveryAndCost.getTaxId());

            if (!result.containsKey(driver)){
                driver.setTenderCode(tenderCode);
                driver.setUniqueCode(deliveryAndCost.getUniqueCode());
                driver.setDenomination(deliveryAndCost.getDenomination());
                driver.setBusinessName(deliveryAndCost.getBusinessName());
                driver.setRegisteredOffice(deliveryAndCost.getRegisteredOffice());
                driver.setPec(deliveryAndCost.getPec());
                driver.setFiscalCode(deliveryAndCost.getFiscalCode());
                driver.setPhoneNumber(deliveryAndCost.getPhoneNumber());
                driver.setUniqueCode(deliveryAndCost.getUniqueCode());
                driver.setFsu(deliveryAndCost.getFsu());
                result.put(driver, costs);
            } else {
                costs = result.get(driver);
            }


            //Create cost object
            PnCost pnCost = new PnCost();
            pnCost.setDeliveryDriverCode(driver.getTaxId());
            pnCost.setZone(deliveryAndCost.getZone());
            pnCost.setUuid(UUID.randomUUID().toString());
            pnCost.setProductType(deliveryAndCost.getProductType());
            pnCost.setBasePrice(deliveryAndCost.getBasePrice());
            pnCost.setPagePrice(deliveryAndCost.getPagePrice());
            pnCost.setCap(deliveryAndCost.getCaps());
            pnCost.setFsu(deliveryAndCost.getFsu());
            pnCost.setTenderCode(tenderCode);

            if (StringUtils.isNotBlank(pnCost.getZone())){
                internationalCosts.add(new ZoneProductType(pnCost.getZone(), pnCost.getProductType(), pnCost.getFsu()));
            } else {
                pnCost.getCap().forEach(cap -> nationalCosts.add(new CapProductType(cap, pnCost.getProductType(), pnCost.getFsu())));
            }

            costs.add(pnCost);

        });


        validInternationalCosts(internationalCosts);

        if (!validNationalCosts(nationalCosts))
            throw new PnExcelValidatorException(ExceptionTypeEnum.INVALID_CAP_PRODUCT_TYPE, null);

        if (!validNationalCostsFSU(nationalCosts))
            throw new PnExcelValidatorException(ExceptionTypeEnum.INVALID_CAP_FSU, null);

        //Check Unique Zone Product Type

        return result;
    }

    public static boolean validNationalCosts(List<CapProductType> nationalCosts) throws PnExcelValidatorException {
        Set<CapProductType> setCosts = new HashSet<>(nationalCosts);
        return setCosts.size() == nationalCosts.size();
    }

    public static boolean validNationalCostsFSU(List<CapProductType> nationalCosts) {
        List<CapProductType> costsFSU = nationalCosts.parallelStream().filter(cost -> cost.isFsu() && cost.getCap().equals(Const.CAP_DEFAULT)).toList();
        AtomicBoolean hasAR = new AtomicBoolean(false);
        AtomicBoolean has890 = new AtomicBoolean(false);
        AtomicBoolean hasRS = new AtomicBoolean(false);
        costsFSU.forEach(cost -> {
            if (cost.getProductType().equalsIgnoreCase(ProductTypeEnumDto.AR.getValue())){
                hasAR.set(true);
            }
            if (cost.getProductType().equalsIgnoreCase(ProductTypeEnumDto._890.getValue())){
                has890.set(true);
            }
            if (cost.getProductType().equalsIgnoreCase(ProductTypeEnumDto.RS.getValue())){
                hasRS.set(true);
            }
        });
        return hasAR.get() && has890.get() && hasRS.get();

    }

    public static void validInternationalCosts(List<ZoneProductType> internationalCosts) throws PnExcelValidatorException {
        Set<ZoneProductType> setCosts = new HashSet<>(internationalCosts);
        if (setCosts.size() < internationalCosts.size()){
            throw new PnExcelValidatorException(ExceptionTypeEnum.INVALID_ZONE_PRODUCT_TYPE, null);
        }
        Map<String, Integer> occurance = new HashMap<>();
        internationalCosts.stream().filter(ZoneProductType::isFsu)
                .forEach(cost-> {
                    String key = cost.getZone() + cost.getProductType();
                    if (!occurance.containsKey(key)){
                        occurance.put(key, 1);
                    }
                });
        if (occurance.keySet().size() != 6) {
            throw new PnExcelValidatorException(ExceptionTypeEnum.INVALID_ZONE_FSU, null);
        }
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
