package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderCreateRequestDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderUploadRequestDto;
import it.pagopa.pn.paperchannel.utils.Const;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    public static PnTender toTender(TenderUploadRequestDto tenderUploadRequestDto){
        PnTender tender = new PnTender();
        String tenderCode = UUID.randomUUID().toString();
        tender.setTenderCode(tenderCode);
        tender.setStatus("CREATED");
        tender.setDate(Instant.now());
        tender.setEndDate(tenderUploadRequestDto.getTender().getEndDate().toInstant());
        tender.setStartDate(tenderUploadRequestDto.getTender().getStartDate().toInstant());
        tender.setAuthor(Const.PN_PAPER_CHANNEL);
        tender.setDescription(tenderUploadRequestDto.getTender().getName());
        return tender;
    }

    public static PnTender toTenderRequest(TenderCreateRequestDTO dto){
        PnTender tender = new PnTender();
        tender.setTenderCode(UUID.randomUUID().toString());
        tender.setStatus("CREATED");
        tender.setDescription(dto.getName());
        tender.setDate(Instant.now());
        tender.setAuthor(Const.PN_PAPER_CHANNEL);
        tender.setStartDate(dto.getStartDate().toInstant());
        tender.setEndDate(dto.getEndDate().toInstant());
        return tender;
    }

    public static TenderDTO tenderToDto(PnTender pnTender) {
        return mapperTenderToDto.toDTO(pnTender);
    }

    public static PageModel<PnTender> toPagination(Pageable pageable, List<PnTender> list){
        return PageModel.builder(list, pageable);
    }

}
