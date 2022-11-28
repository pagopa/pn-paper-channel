package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;

public class RequestDeliveryMapper {


    public static RequestDeliveryEntity toEntity(String requestId, String fiscalCode, String addressHash){
        RequestDeliveryEntity entity = new RequestDeliveryEntity();
        entity.setRequestId(requestId);
        entity.setFiscalCode(fiscalCode);
        entity.setAddressHash(addressHash);
        return entity;
    }

}
