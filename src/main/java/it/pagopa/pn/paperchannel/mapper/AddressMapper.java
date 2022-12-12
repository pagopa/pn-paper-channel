package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;

public class AddressMapper {

    private static final BaseMapper<Address, AnalogAddress> mapper = new BaseMapperImpl(Address.class, AnalogAddress.class);

    private AddressMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static Address fromAnalogToAddress(AnalogAddress analogAddress){
        return mapper.toEntity(analogAddress);
    }

}
