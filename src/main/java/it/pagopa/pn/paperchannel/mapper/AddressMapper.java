package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.time.LocalDateTime;

public class AddressMapper {

    private static final BaseMapper<Address, AnalogAddress> mapper = new BaseMapperImpl(Address.class, AnalogAddress.class);

    private static final BaseMapper<PnAddress, Address> mapperToAddressEntity = new BaseMapperImpl(PnAddress.class, Address.class);

    private AddressMapper(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static Address fromAnalogToAddress(AnalogAddress analogAddress){
        return mapper.toEntity(analogAddress);
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

}
