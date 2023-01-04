package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.time.LocalDateTime;

public class AddressMapper {
    private static final BaseMapper<Address, AnalogAddress> mapperAnalog = new BaseMapperImpl(Address.class, AnalogAddress.class);

    private static final BaseMapper<PnAddress, Address> mapperToAddressEntity = new BaseMapperImpl(PnAddress.class, Address.class);

    private static final BaseMapper<AnalogAddress, DiscoveredAddressDto> mapperToAnalog = new BaseMapperImpl(AnalogAddress.class, DiscoveredAddressDto.class);

    private AddressMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static Address fromAnalogToAddress(AnalogAddress analogAddress){
        if (analogAddress == null) return null;
        return mapperAnalog.toEntity(analogAddress);
    }

    public static Address fromNationalRegistry(AddressSQSMessagePhysicalAddressDto pysicalAddress){
        Address address = new Address();
        address.setFullName(pysicalAddress.getAt());
        address.setAddress(pysicalAddress.getAddress());
        address.setAddressRow2(pysicalAddress.getAddressDetails());
        address.setCap(pysicalAddress.getZip());
        address.setCity(pysicalAddress.getMunicipality());
        address.setCity2(pysicalAddress.getMunicipalityDetails());
        address.setPr(pysicalAddress.getProvince());
        address.setCountry(pysicalAddress.getForeignState());
        return address;
    }

    public static PnAddress toEntity(Address address, String requestId){
        PnAddress pnAddress = mapperToAddressEntity.toEntity(address);
        pnAddress.setRequestId(requestId);
        pnAddress.setTtl(DateUtils.getTimeStampOfMills(LocalDateTime.now().plusMinutes(30L)));
        return pnAddress;
    }

    public static Address toDTO(PnAddress address){
        return mapperToAddressEntity.toDTO(address);
    }

    public static AnalogAddress toPojo(DiscoveredAddressDto discoveredAddressDto){
        return mapperToAnalog.toEntity(discoveredAddressDto);
    }


}
