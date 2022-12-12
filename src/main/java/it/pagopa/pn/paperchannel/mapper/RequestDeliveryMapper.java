package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.pojo.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;

public class RequestDeliveryMapper {
    private static final BaseMapper<AddressEntity, Address> mapperAddress = new BaseMapperImpl(AddressEntity.class, AnalogAddress.class);
    private RequestDeliveryMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static RequestDeliveryEntity toEntity(PrepareRequest request, Address address,String correlationId){
        RequestDeliveryEntity entity = new RequestDeliveryEntity();
        entity.setRequestId(request.getRequestId());
        entity.setRegisteredLetterCode(request.getProductType());
        entity.setStartDate(DateUtils.formatDate(new Date()));
        entity.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        entity.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDescription());
        entity.setStatusDate(DateUtils.formatDate(new Date()));
        entity.setFiscalCode(request.getReceiverFiscalCode());
        entity.setAddressHash("Hash code");

        if(address!=null){
            entity.setAddress(mapperAddress.toEntity(address));
        }
       // entity.setCorrelationId(correlationId);
        return entity;
    }

}
