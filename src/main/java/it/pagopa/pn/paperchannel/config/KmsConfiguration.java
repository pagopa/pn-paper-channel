package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.encryption.impl.KmsEncryptionImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

import java.net.URI;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class KmsConfiguration {

    private final AwsKmsProperties properties;
    private final AwsConfigs awsConfigs;


    @Bean
    public KmsClient kmsClient() {
        var builder = KmsClient.builder();

        if(System.getenv("AWS_REGIONCODE") == null) {
            if (Optional.ofNullable(awsConfigs.getEndpointUrl()).isPresent()) {
                builder.endpointOverride(URI.create(awsConfigs.getEndpointUrl()));
                Optional.ofNullable(awsConfigs.getRegionCode()).ifPresent(r -> builder.region(Region.of(r)));
            } else {
                Optional.ofNullable(awsConfigs.getRegionCode()).ifPresent(r -> builder.region(Region.of(r)));
            }
        }

        return builder.build();
    }

    @Bean
    @Qualifier("kmsEncryption")
    public DataEncryption kmsEncryption(KmsClient kmsClient){
        return new KmsEncryptionImpl(kmsClient, this.properties);
    }
}
