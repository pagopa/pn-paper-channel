package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperTender;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class TenderMapper {
    private TenderMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnPaperTender, TenderDTO> mapperTenderToDto = new BaseMapperImpl<>(PnPaperTender.class, TenderDTO.class);

    public static PageableTenderResponseDto toPageableResponse(PageModel<PnPaperTender> pagePnPaperTender) {
        PageableTenderResponseDto pageableTenderResponseDto = new PageableTenderResponseDto();
        pageableTenderResponseDto.setPageable(pagePnPaperTender.getPageable());
        pageableTenderResponseDto.setNumber(pagePnPaperTender.getNumber());
        pageableTenderResponseDto.setNumberOfElements(pagePnPaperTender.getNumberOfElements());
        pageableTenderResponseDto.setSize(pagePnPaperTender.getSize());
        pageableTenderResponseDto.setTotalElements(pagePnPaperTender.getTotalElements());
        pageableTenderResponseDto.setTotalPages((long) pagePnPaperTender.getTotalPages());
        pageableTenderResponseDto.setFirst(pagePnPaperTender.isFirst());
        pageableTenderResponseDto.setLast(pagePnPaperTender.isLast());
        pageableTenderResponseDto.setEmpty(pagePnPaperTender.isEmpty());
        pageableTenderResponseDto.setContent(pagePnPaperTender.mapTo(TenderMapper::tenderToDto));
        return pageableTenderResponseDto;
    }

    public static TenderDTO tenderToDto(PnPaperTender pnPaperTender) {
        return mapperTenderToDto.toDTO(pnPaperTender);
    }

    public static PageModel<PnPaperTender> toPagination(Pageable pageable, List<PnPaperTender> list){
        return PageModel.builder(list, pageable);
    }

}
