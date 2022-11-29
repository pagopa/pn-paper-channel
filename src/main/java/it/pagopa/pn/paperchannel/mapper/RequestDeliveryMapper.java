package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.Address;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;

public class RequestDeliveryMapper {
    private static final BaseMapper<Address, AnalogAddress> mapperAddress = new BaseMapperImpl(Address.class, AnalogAddress.class);
    private RequestDeliveryMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static RequestDeliveryEntity toEntity(PrepareRequest request){
        RequestDeliveryEntity entity = new RequestDeliveryEntity();
        entity.setRequestId(request.getRequestId());
        entity.setFiscalCode(request.getReceiverFiscalCode());
        entity.setAddressHash("Hash code");
        entity.setAddress(mapperAddress.toEntity(request.getReceiverAddress()));
        return entity;
    }

}
