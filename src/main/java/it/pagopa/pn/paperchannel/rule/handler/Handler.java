package it.pagopa.pn.paperchannel.rule.handler;

import it.pagopa.pn.paperchannel.rule.model.*;

public abstract class Handler<T, C, R extends ResultFilter> {

    protected Handler<T, C, R> nextHandler;

    abstract R filter(T item, C ruleContext);

    void setNext(Handler<T, C, R> nextHandler){
        this.nextHandler = nextHandler;
    }
}
