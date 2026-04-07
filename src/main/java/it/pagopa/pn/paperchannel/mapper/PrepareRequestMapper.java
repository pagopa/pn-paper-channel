package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.model.CommunicationType;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = CommunicationType.class)
public interface PrepareRequestMapper {

    @Mapping(target = "communicationType", expression = "java(CommunicationType.LEGAL)")
    @Mapping(target = "clientId", source = "clientId")
    PrepareRequestInt prepareRequestToInternal(PrepareRequest request, String clientId);
}
