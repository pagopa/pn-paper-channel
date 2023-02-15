package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
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
        List<CapDto> lst = new ArrayList<>();
        paperCosts.forEach(i -> {
            lst.add(fromEntity(i));
        });
        responseDto.setContent(lst);
        return responseDto;
    }
}
