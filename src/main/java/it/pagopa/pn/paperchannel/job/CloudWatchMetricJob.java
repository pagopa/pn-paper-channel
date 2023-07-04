package it.pagopa.pn.paperchannel.job;

import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.util.Collections;

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
        createAndSendMetric(NAMESPACE_CW_PDV, "PNPaperErrorRequest");
    }

    private void createAndSendMetric(String namespace, String metricName) {
        this.paperRequestErrorDAO.findAll()
                .flatMap(result -> {
                    MetricDatum metricDatum = MetricDatum.builder()
                            .metricName(metricName)
                            .value(result.size() > 0 ? (double) 1 : 0)
                            .unit(StandardUnit.COUNT)
                            .timestamp(Instant.now())
                            .build();
                    log.debug("createAndSendMetric metricDatanum created");
                    PutMetricDataRequest metricDataRequest = PutMetricDataRequest.builder()
                            .namespace(namespace)
                            .metricData(Collections.singletonList(metricDatum))
                            .build();
                    log.debug("createAndSendMetric metricDataRequest created");
                    return  Mono.fromFuture(cloudWatchAsyncClient.putMetricData(metricDataRequest));
                })
                .subscribe(putMetricDataResponse -> log.debug("[{}] PutMetricDataResponse: {}", namespace, putMetricDataResponse),
                        throwable -> log.warn(String.format("[%s] Error sending metric", namespace), throwable));
    }
}
