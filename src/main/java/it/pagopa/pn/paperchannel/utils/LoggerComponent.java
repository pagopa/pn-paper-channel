package it.pagopa.pn.paperchannel.utils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class LoggerComponent {
    private static final Logger logger = LoggerFactory.getLogger(LoggerComponent.class);

    @Pointcut("within(it.pagopa.pn.paperchannel.rest.v1..*) " +
            "&& @annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {
    }

    //aggiungere altro pointcut per i client quando ci sono chiamate a servizi esterni

    @Before("pointcut()")
    public void logMethod(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        logger.info("Invoked operationId {} with args: {}", args[0], args );
    }

    @After("pointcut()")
    public void logMethodAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("Successful API operation with methodName {} = result {}", methodName, args[1]);
    }

}
