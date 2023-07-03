package it.pagopa.pn.paperchannel.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("aws.bucket")
public class AwsBucketProperties {

    private String name;
    private long expiration;

}
