package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ErrorDetailMapStructMapper {

    PnErrorDetails toEntity(PnExcelValidatorException.ErrorCell dto);
}
