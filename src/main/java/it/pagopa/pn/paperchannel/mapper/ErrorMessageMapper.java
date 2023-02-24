package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ErrorMessageMapper {

    private ErrorMessageMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static final BaseMapper<PnErrorMessage, PnExcelValidatorException> mapper = new BaseMapperImpl<>(PnErrorMessage.class,PnExcelValidatorException.class);

    public static PnErrorMessage toEntity(PnExcelValidatorException dto){
        PnErrorMessage entity = new PnErrorMessage();
        entity.setMessage(dto.getErrorType().getMessage());
        List<PnErrorDetails> detailsList = new ArrayList<>();
        if (dto.getErrors() != null){
            dto.getErrors().forEach(errorCell -> detailsList.add(ErrorDetailMapper.toEntity(errorCell)));
        }
        entity.setErrorDetails(detailsList);
        return entity;
    }

}
