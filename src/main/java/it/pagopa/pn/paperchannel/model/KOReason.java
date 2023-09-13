package it.pagopa.pn.paperchannel.model;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;

public record KOReason(FailureDetailCodeEnum failureDetailCode, Address addressFailed) {}
