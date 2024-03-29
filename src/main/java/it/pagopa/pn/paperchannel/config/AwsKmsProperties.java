package it.pagopa.pn.paperchannel.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("aws.kms")
public class AwsKmsProperties {

    private String keyId;

}
