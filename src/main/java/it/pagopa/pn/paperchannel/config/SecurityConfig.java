package it.pagopa.pn.paperchannel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public WebFilter strictTransportSecurity() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            exchange.getResponse().getHeaders()
                    .add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            return chain.filter(exchange);
        };
    }
}