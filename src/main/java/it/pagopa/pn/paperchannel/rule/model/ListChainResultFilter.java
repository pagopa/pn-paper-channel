package it.pagopa.pn.paperchannel.rule.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ListChainResultFilter<T> extends ResultFilter {

    private T item;
}
