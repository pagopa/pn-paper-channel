package it.pagopa.pn.paperchannel.job;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;


@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CloudWatchMetricJob {

    private static final String NAMESPACE_CW_PDV = "pn-paper-channel";

    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    private final PaperRequestErrorDAO paperRequestErrorDAO;


    @Scheduled(cron = "${pn.paper-channel.cloudwatch-metric-cron}")
    public void sendMetricToCloudWatch() {
        createAndSendMetric( NAMESPACE_CW_PDV, "PNPaperErrorRequest");

    }

    private void createAndSendMetric(String namespace, String metricName) {
        this.paperRequestErrorDAO.findAll()
                .flatMap(result -> {
                    // Operazioni da eseguire quando il flusso non è vuoto
                    MetricDatum metricDatum = MetricDatum.builder()
                            .metricName(metricName)
                            .unit(StandardUnit.COUNT)
                            .dimensions(Collections.singletonList(Dimension.builder()
                                    .name("Environment")
                                    .build()))
                            .timestamp(Instant.now())
                            .build();

                    PutMetricDataRequest metricDataRequest = PutMetricDataRequest.builder()
                            .namespace(namespace)
                            .metricData(Collections.singletonList(metricDatum))
                            .build();

                    return  Mono.fromFuture(cloudWatchAsyncClient.putMetricData(metricDataRequest));
                })
                .subscribe(putMetricDataResponse -> log.trace("[{}] PutMetricDataResponse: {}", namespace, putMetricDataResponse),
                        throwable -> log.warn(String.format("[%s] Error sending metric", namespace), throwable));
    }
}
