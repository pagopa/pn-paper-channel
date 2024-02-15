package it.pagopa.pn.paperchannel;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KMS;

/**
 * Classe che permette di creare un container Docker di LocalStack.
 * Il container (e quindi la classe) può essere condivisa tra più classi di test.
 * Per utilizzare questa classe, le classi di test dovranno essere annotate con
 * @Import(LocalStackTestConfig.class)
 */
@TestConfiguration
@Slf4j
public class LocalStackTestConfig {
    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.4").asCompatibleSubstituteFor("localstack/localstack"))
                    .withServices(DYNAMODB)
                    .withClasspathResourceMapping("testcontainers/init.sh",
                            "/docker-entrypoint-initaws.d/make-storages.sh", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("testcontainers/credentials",
                            "/root/.aws/credentials", BindMode.READ_ONLY)
                    .withNetworkAliases("localstack")
                    .withNetwork(Network.builder().build())
                    .withStartupTimeout(Duration.ofSeconds(10))
                    .withStartupAttempts(3)
                    .waitingFor(Wait.forLogMessage(".*Initialization terminated.*", 1));

    static {
        localStack.start();
        System.setProperty("aws.kms.keyId", kmsKeyCreation(localStack));
        System.setProperty("aws.endpoint-url", localStack.getEndpointOverride(DYNAMODB).toString());
        createSqsQueuesTest();

        try {
            System.setProperty("aws.sharedCredentialsFile", new ClassPathResource("testcontainers/credentials").getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static String kmsKeyCreation(LocalStackContainer localstack) {
        AWSKMS awskms = AWSKMSClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                localstack.getEndpointOverride(LocalStackContainer.Service.KMS).toString(),
                                localstack.getRegion()
                        )
                )
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .build();

        String desc = String.format("AWS CMK Description");
        Tag createdByTag = new Tag().withTagKey("CreatedBy").withTagValue("StorageService");
        CreateKeyRequest req = new CreateKeyRequest().withDescription(desc).withTags(createdByTag);
        CreateKeyResult key = awskms.createKey(req);
        log.info("Arn : {}", key.getKeyMetadata().getArn());
        return key.getKeyMetadata().getArn();
    }

    public static void createSqsQueuesTest() {
        var extChannelQueueName = "local-ext-channels-outputs-test";
        var extChannelDLQQueueName = extChannelQueueName + "-DLQ";

        AmazonSQS sqs = AmazonSQSClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString(),
                                localStack.getRegion()
                        )
                )
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(localStack.getAccessKey(), localStack.getSecretKey())
                        )
                )
                .build();

        CreateQueueRequest extChannelQueueDLQ = new CreateQueueRequest("")
                .withQueueName(extChannelDLQQueueName);

        CreateQueueRequest extChannelQueue = new CreateQueueRequest("")
                .withQueueName(extChannelQueueName)
                .addAttributesEntry("DelaySeconds", "0")
                .addAttributesEntry("VisibilityTimeout", "5")
                .addAttributesEntry("RedrivePolicy", "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:" + extChannelDLQQueueName + "\",\"maxReceiveCount\":\"2\"}");

        try {
            sqs.createQueue(extChannelQueue);
            sqs.createQueue(extChannelQueueDLQ);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }
    }


}
