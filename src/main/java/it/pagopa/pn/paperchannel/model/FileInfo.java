package it.pagopa.pn.paperchannel.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileInfo {

    private byte[] data;
    private String url;

}
