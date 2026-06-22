package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PnDiscoveredAddressMapper {
    PnDiscoveredAddressMapper INSTANCE = Mappers.getMapper(PnDiscoveredAddressMapper.class);

    DiscoveredAddressDto toDiscoveredAddressDto(PnDiscoveredAddress discoveredAddress);

    PnDiscoveredAddress toPnDiscoveredAddress(DiscoveredAddressDto discoveredAddressDto);
}
