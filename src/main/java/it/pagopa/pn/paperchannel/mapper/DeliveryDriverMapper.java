package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

public class DeliveryDriverMapper {

    private static final BaseMapper<PnPaperDeliveryDriver, DeliveryDriverDto> mapperDeliveryDriverToDto = new BaseMapperImpl<>(PnPaperDeliveryDriver.class, DeliveryDriverDto.class);

    public static PnPaperDeliveryDriver toContractRequest(ContractInsertRequestDto contractInsertRequestDto){
        PnPaperDeliveryDriver contractRequest = new PnPaperDeliveryDriver();
        contractRequest.setUniqueCode(contractInsertRequestDto.getUniqueCode());
        contractRequest.setDenomination(contractInsertRequestDto.getDenomination());
        contractRequest.setTaxId(contractInsertRequestDto.getTaxId());
        contractRequest.setPhoneNumber(contractInsertRequestDto.getPhoneNumber());
        contractRequest.setFsu(contractInsertRequestDto.getFsu());
        return contractRequest;
    }

    public static PageableDeliveryDriverResponseDto deliveryDriverToPageableDeliveryDriverDto(PageModel<PnPaperDeliveryDriver> pagePnPaperDeliveryDriver) {
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

    public static PageModel<PnPaperDeliveryDriver> paginateList(Pageable pageable, List<PnPaperDeliveryDriver> list) {
        return PageModel.builder(list, pageable);
    }

}
