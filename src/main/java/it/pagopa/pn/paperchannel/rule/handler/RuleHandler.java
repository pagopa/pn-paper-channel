package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.*;

import java.util.List;

public abstract class RuleHandler<T, C, R extends ResultFilter> {

    protected RuleHandler<T, C, R> nextHandler;

    abstract R filter(C ruleContext);

    void setNext(RuleHandler<T, C, R> nextHandler){
        this.nextHandler = nextHandler;
    }
}
