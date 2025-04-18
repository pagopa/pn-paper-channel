package it.pagopa.pn.paperchannel.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public WebFilter strictTransportSecurity() {
            return new SecurityConfig().strictTransportSecurity();
        }
    }

    @Autowired
    private WebFilter strictTransportSecurity;

    @Test
    void shouldAddStrictTransportSecurityHeader() {
        // Arrange
        WebFilterChain mockChain = mock(WebFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").build()
        );

        // Act
        strictTransportSecurity.filter(exchange, mockChain).block();

        // Assert
        String headerValue = exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security");
        assertNotNull(headerValue);
        assertEquals("max-age=31536000; includeSubDomains; preload", headerValue);
        verify(mockChain).filter(exchange);
    }
}