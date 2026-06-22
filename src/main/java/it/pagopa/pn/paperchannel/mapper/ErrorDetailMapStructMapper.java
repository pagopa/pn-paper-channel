package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ErrorDetailMapStructMapper {
    ErrorDetailMapStructMapper INSTANCE = Mappers.getMapper(ErrorDetailMapStructMapper.class);

    PnErrorDetails toEntity(PnExcelValidatorException.ErrorCell dto);
}
