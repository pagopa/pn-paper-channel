package it.pagopa.pn.paperchannel.model;

import lombok.Getter;

@Getter
public enum FileStatusCodeEnum {
    UPLOADING("UPLOADING"),UPLOADED("UPLOADED"),ERROR("ERROR"), IN_PROGRESS("IN_PROGRESS"), COMPLETE("COMPLETE");

    private final String code;

    FileStatusCodeEnum(String code) {
        this.code = code;
    }
}
