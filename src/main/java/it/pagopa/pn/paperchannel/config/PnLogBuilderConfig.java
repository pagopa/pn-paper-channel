package it.pagopa.pn.paperchannel.config;


import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class PnLogBuilderConfig {

    @Bean
    public PnAuditLogBuilder pnAuditLogBuilder(){
        return new PnAuditLogBuilder();
    }

}
