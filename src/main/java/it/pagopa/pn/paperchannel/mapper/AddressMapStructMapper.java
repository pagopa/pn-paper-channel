package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddressMapStructMapper {
	AddressMapStructMapper INSTANCE = Mappers.getMapper(AddressMapStructMapper.class);

	@Mapping(target = "fullName", source = "fullname")
	@Mapping(target = "flowType", ignore = true)
	@Mapping(target = "productType", ignore = true)
	@Mapping(target = "fromNationalRegistry", ignore = true)
	Address analogAddressToAddress(AnalogAddress analogAddress);

	@Mapping(target = "fullname", source = "fullName")
	AnalogAddress addressToAnalogAddress(Address address);

	@Mapping(target = "requestId", ignore = true)
	@Mapping(target = "ttl", ignore = true)
	@Mapping(target = "typology", ignore = true)
	PnAddress addressToPnAddress(Address address);

	@Mapping(target = "flowType", ignore = true)
	@Mapping(target = "productType", ignore = true)
	@Mapping(target = "fromNationalRegistry", ignore = true)
	Address pnAddressToAddress(PnAddress address);

	@Mapping(target = "fullname", source = "fullName")
	AnalogAddress pnAddressToAnalogAddress(PnAddress address);

	@Mapping(target = "fullname", source = "name")
	AnalogAddress discoveredAddressToAnalogAddress(DiscoveredAddressDto discoveredAddressDto);
}

