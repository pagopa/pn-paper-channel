package it.pagopa.pn.paperchannel.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.encryption.impl.KmsEncryptionImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class KmsConfiguration {

    private final AwsKmsProperties properties;
    private final AwsConfigs awsConfigs;


    @Bean
    public AWSKMS kms() {
        final AWSKMSClientBuilder builder = AWSKMSClient.builder();

        if (Optional.ofNullable(awsConfigs.getEndpointUrl()).isPresent()) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()));
        } else {
            Optional.ofNullable(awsConfigs.getRegionCode()).ifPresent(builder::setRegion);
        }

        return builder.build();
    }

    @Bean
    @Qualifier("kmsEncryption")
    public DataEncryption kmsEncryption(AWSKMS awskms){
        return new KmsEncryptionImpl(awskms, this.properties);
    }
}
