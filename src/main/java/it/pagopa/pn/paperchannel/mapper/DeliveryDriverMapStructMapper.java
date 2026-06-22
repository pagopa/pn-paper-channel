package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeliveryDriverMapStructMapper {
    DeliveryDriverMapStructMapper INSTANCE = Mappers.getMapper(DeliveryDriverMapStructMapper.class);

    @Mapping(target = "tenderCode", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    PnDeliveryDriver toEntity(DeliveryDriverDTO dto);

    DeliveryDriverDTO toDto(PnDeliveryDriver entity);
}
