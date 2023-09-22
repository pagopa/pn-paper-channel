package it.pagopa.pn.paperchannel.model;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.FailureDetailCodeEnum;

import java.io.Serializable;

public record KOReason(FailureDetailCodeEnum failureDetailCode, Address addressFailed) implements Serializable {}
