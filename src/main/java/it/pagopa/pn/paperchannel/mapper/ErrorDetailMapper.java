package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ErrorDetailMapper {

    private ErrorDetailMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static final BaseMapper<PnErrorDetails, PnExcelValidatorException.ErrorCell> mapper = new BaseMapperImpl<>(PnErrorDetails.class,PnExcelValidatorException.ErrorCell.class);

    public static PnErrorDetails toEntity(PnExcelValidatorException.ErrorCell dto){
        return mapper.toEntity(dto);
    }

    public static PnExcelValidatorException.ErrorCell toDto(PnErrorDetails entity){
        return new PnExcelValidatorException.ErrorCell(entity.getRow(), entity.getCol(), entity.getMessage());
    }

    public static List<PnExcelValidatorException.ErrorCell> toDtos(List<PnErrorDetails> entity){
        List<PnExcelValidatorException.ErrorCell> list = new ArrayList<>();
        entity.forEach(e -> {
            list.add(toDto(e));
        });
        return list;
    }

}
