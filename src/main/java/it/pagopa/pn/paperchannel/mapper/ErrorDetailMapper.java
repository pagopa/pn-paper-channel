package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorDetailMapper {

    private ErrorDetailMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static final BaseMapper<PnErrorDetails, PnExcelValidatorException.ErrorCell> mapper = new BaseMapperImpl<>(PnErrorDetails.class,PnExcelValidatorException.ErrorCell.class);

    public static PnErrorDetails toEntity(PnExcelValidatorException.ErrorCell dto){
        return mapper.toEntity(dto);
    }

}
