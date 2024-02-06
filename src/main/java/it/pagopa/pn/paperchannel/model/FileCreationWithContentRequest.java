package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreationWithContentRequest extends FileCreationRequestDto {

    private byte[] content;
}
