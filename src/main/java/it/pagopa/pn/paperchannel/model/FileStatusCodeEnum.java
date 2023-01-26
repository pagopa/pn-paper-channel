package it.pagopa.pn.paperchannel.model;

import lombok.Getter;

@Getter
public enum FileStatusCodeEnum {
    UPLOADING("UPLOADED"),UPLOADED("UPLOADED");

    private final String code;

    FileStatusCodeEnum(String code) {
        this.code = code;
    }
}
