package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.model.CommunicationTypeEnum;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = CommunicationTypeEnum.class)
public interface PrepareRequestMapper {

    @Mapping(target = "communicationType", expression = "java(CommunicationTypeEnum.LEGAL.name())")
    @Mapping(target = "clientId", source = "clientId")
    PrepareRequestInt prepareRequestToInternal(PrepareRequest request, String clientId);
}
