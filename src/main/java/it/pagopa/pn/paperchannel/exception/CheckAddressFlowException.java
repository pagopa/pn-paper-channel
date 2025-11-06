package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum;
import lombok.Getter;

@Getter
public class CheckAddressFlowException extends RuntimeException {
    private final String geoKey;
    private final ErrorFlowTypeEnum flowTypeEnum;

  public CheckAddressFlowException(Throwable ex, String geoKey) {
    super(ex.getMessage());
    this.flowTypeEnum = ErrorFlowTypeEnum.CHECK_ADDRESS_FLOW;
    this.geoKey = geoKey;
  }
}
