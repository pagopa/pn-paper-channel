package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnPaperGeoKeyDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelGeoKey;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.Comparator;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.GEOKEY_NOT_FOUND;


@Repository
public class PnPaperGeoKeyDAOImpl extends BaseDAO<PnPaperChannelGeoKey> implements PnPaperGeoKeyDAO {


    public PnPaperGeoKeyDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                DynamoDbAsyncClient dynamoDbAsyncClient,
                                AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperChannelGeoKeyTable(), PnPaperChannelGeoKey.class);
    }

    @Override
    public Mono<PnPaperChannelGeoKey> createOrUpdate(PnPaperChannelGeoKey paperChannelGeoKey) {
        return Mono.fromFuture(put(paperChannelGeoKey).thenApply(item -> item));
    }


    /**
     * Retrieve the active GeoKey
     *
     * @return  the entity of GeoKey
     **/
    @Override
    public Mono<PnPaperChannelGeoKey> getGeoKey(String tenderId, String product, String geoKey) {

        var paperChannelGeoKey = new PnPaperChannelGeoKey(tenderId, product, geoKey);

        QueryConditional keyConditional = CONDITION_BETWEEN.apply(
                new Keys(
                        Key.builder()
                                .partitionValue(paperChannelGeoKey.getTenderProductGeokey())
                                .sortValue(Instant.EPOCH.toString())  // La data minima possibile
                                .build(),
                        Key.builder()
                                .partitionValue(paperChannelGeoKey.getTenderProductGeokey())
                                .sortValue(Instant.now().toString())  // La data corrente
                                .build()
                )
        );

        return super.getByFilter(keyConditional, null, null, null, null, false)
                .sort(Comparator.comparing(PnPaperChannelGeoKey::getActivationDate).reversed())
                .next()
                .flatMap(item -> {
                    if (item.getDismissed().equals(Boolean.TRUE))
                        return Mono.error(new PnGenericException(GEOKEY_NOT_FOUND, GEOKEY_NOT_FOUND.getMessage()));
                    return Mono.just(item);
                });
    }



}
