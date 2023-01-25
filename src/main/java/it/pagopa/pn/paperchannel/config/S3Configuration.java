package it.pagopa.pn.paperchannel.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.s3.impl.S3BucketImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class S3Configuration {

    private final AwsPropertiesConfig awsConfigs;
    private final AwsBucketProperties awsBucketProperties;

    public S3Configuration(AwsPropertiesConfig awsConfigs, AwsBucketProperties awsBucketProperties) {
        this.awsConfigs = awsConfigs;
        this.awsBucketProperties = awsBucketProperties;
    }

    @Bean
    public AmazonS3 amazonS3() {
        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else {
            return AmazonS3ClientBuilder.standard()
                    .withRegion(awsConfigs.getRegionCode())
                    .build();
        }
    }

    @Bean
    public S3Bucket s3Client(AmazonS3 amazonS3){
        return new S3BucketImpl(amazonS3, this.awsBucketProperties);
    }
}