package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.TenderCreateRequestDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.utils.Const;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TenderMapper {
    private TenderMapper() {
        throw new IllegalCallerException();
    }

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

    public static PnTender toTenderRequest(TenderCreateRequestDTO dto){
        PnTender tender = new PnTender();
        tender.setTenderCode(dto.getCode());
        if (tender.getTenderCode() == null){
            tender.setTenderCode(UUID.randomUUID().toString());
        }
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());
        tender.setDescription(dto.getName());
        tender.setDate(Instant.now());
        tender.setAuthor(Const.PN_PAPER_CHANNEL);
        tender.setStartDate(dto.getStartDate().toInstant());
        tender.setEndDate(dto.getEndDate().toInstant());
        return tender;
    }

    public static TenderDTO tenderToDto(PnTender pnTender) {
        TenderDTO tender = new TenderDTO();
        tender.setCode(pnTender.getTenderCode());
        tender.setName(pnTender.getDescription());
        tender.setStatus(TenderDTO.StatusEnum.fromValue(pnTender.getActualStatus()));
        if (pnTender.getStartDate() != null) tender.setStartDate(Date.from(pnTender.getStartDate()));
        if (pnTender.getEndDate() != null) tender.setEndDate(Date.from(pnTender.getEndDate()));
        return tender;
    }

    public static PageModel<PnTender> toPagination(Pageable pageable, List<PnTender> list){
        return PageModel.builder(list, pageable);
    }

}
