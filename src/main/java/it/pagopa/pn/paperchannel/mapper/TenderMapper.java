package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class TenderMapper {
    private TenderMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnTender, TenderDTO> mapperTenderToDto = new BaseMapperImpl<>(PnTender.class, TenderDTO.class);

    public static PageableTenderResponseDto toPageableResponse(PageModel<PnTender> pagePnPaperTender) {
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

    public static TenderDTO tenderToDto(PnTender pnTender) {
        return mapperTenderToDto.toDTO(pnTender);
    }

    public static PageModel<PnTender> toPagination(Pageable pageable, List<PnTender> list){
        return PageModel.builder(list, pageable);
    }

}
