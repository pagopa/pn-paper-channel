package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;

import java.util.List;


public class CapMapper {

    private CapMapper(){
        throw new IllegalCallerException();
    }

    public static CapDto fromEntity(PnCap cap){
        CapDto capDto = new CapDto();
        capDto.setCap(cap.getCap());
        return capDto;
    }

    public static CapResponseDto toResponse(List<PnCap> paperCosts){
        CapResponseDto responseDto = new CapResponseDto();
        responseDto.setContent(paperCosts.stream().map(CapMapper::fromEntity).toList());
        return responseDto;
    }
}
