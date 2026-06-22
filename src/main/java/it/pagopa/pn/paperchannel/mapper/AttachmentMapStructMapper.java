package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttachmentMapStructMapper {
    AttachmentMapStructMapper INSTANCE = Mappers.getMapper(AttachmentMapStructMapper.class);

    @Mapping(target = "sha256", source = "checksum")
    AttachmentInfo toAttachmentInfo(PnAttachmentInfo entity);

    @Mapping(target = "checksum", source = "sha256")
    PnAttachmentInfo toPnAttachmentInfo(AttachmentInfo dto);
}
