package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.Status;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

@Repository
public class TenderDAOImpl extends BaseDAO<PnTender> implements TenderDAO {


    private final DynamoDbAsyncTable<PnDeliveryDriver> deliveryDriverTable;
    private final DynamoDbAsyncTable<PnCost> costTable;

    private final TransactWriterInitializer transactWriterInitializer;

    public TenderDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                         DynamoDbAsyncClient dynamoDbAsyncClient,
                         AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbTenderTable(), PnTender.class);
        this.transactWriterInitializer = transactWriterInitializer;
        this.deliveryDriverTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbDeliveryDriverTable(), TableSchema.fromBean(PnDeliveryDriver.class));
        this.costTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbCostTable(), TableSchema.fromBean(PnCost.class));
    }

    @Override
    public Mono<List<PnTender>> getTenders() {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(null, null);

        QueryConditional conditional = CONDITION_BETWEEN.apply(
                new Keys(keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getFirst().toString()),
                        keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getSecond().toString()) )
        );

        return this.getByFilter(conditional, PnTender.AUTHOR_INDEX, null, null)
                .collectList();
    }

    @Override
    public Mono<PnTender> getTender(String tenderCode) {
        return Mono.fromFuture(this.get(tenderCode, null).thenApply(item -> item));
    }

    @Override
    public Mono<PnTender> findActiveTender() {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(null, null);

        QueryConditional conditional = CONDITION_BETWEEN.apply(
                new Keys(keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getFirst().toString()),
                        keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getSecond().toString()) )
        );

        String filter = PnTender.COL_STATUS + " = :activeStatus AND "
                + PnTender.COL_START_DATE + " <= :dateNow AND " + PnTender.COL_END_DATE + " >= :dateNow";

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":activeStatus", AttributeValue.builder().s(TenderDTO.StatusEnum.VALIDATED.getValue()).build());
        values.put(":dateNow", AttributeValue.builder().s(Instant.now().toString()).build());

        return this.getByFilter(conditional, PnTender.AUTHOR_INDEX, values, filter)
                .collectList()
                .flatMap(list -> {
                    if (list == null || list.isEmpty()){
                        return Mono.empty();
                    }
                    return Mono.just(list.get(0));
                });
    }

    @Override
    public Mono<PnTender> createOrUpdate(PnTender tender) {
        return Mono.fromFuture(put(tender).thenApply(i -> tender));
    }

    public Mono<PnTender> createNewContract(Map<PnDeliveryDriver, List<PnCost>> deliveriesAndCost, PnTender tender) {
        this.transactWriterInitializer.init();
        transactWriterInitializer.addRequestTransaction(this.dynamoTable, tender, PnTender.class);
        List<PnDeliveryDriver> deliveries = deliveriesAndCost.keySet().stream().toList();
        deliveries.forEach(delivery -> {
            delivery.setStartDate(Instant.now());
            delivery.setAuthor(Const.PN_PAPER_CHANNEL);
            transactWriterInitializer.addRequestTransaction(deliveryDriverTable, delivery, PnDeliveryDriver.class);
            List<PnCost> costs = deliveriesAndCost.get(delivery);
            costs.forEach(cost -> transactWriterInitializer.addRequestTransaction(this.costTable, cost, PnCost.class));
        });

        return Mono.fromFuture(putWithTransact(transactWriterInitializer.build()).thenApply(item -> tender));
    }
    public Mono<PnTender> deleteTender(String tenderCode){
        return Mono.fromFuture(this.delete(tenderCode, null).thenApply(item -> item));
    }

    @Override
    public Mono<PnTender> getConsolidate(Date startDate, Date endDate) {

        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(null, null);

        QueryConditional conditional = CONDITION_BETWEEN.apply(
                new Keys(keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getFirst().toString()),
                        keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getSecond().toString()) )
        );

        String filter = PnTender.COL_STATUS + " = :consolidateStatus AND (("
                + PnTender.COL_START_DATE + " <= :startDate AND " + PnTender.COL_END_DATE + " >= :endDate ) OR ("
                + PnTender.COL_START_DATE + " <= :startDate AND " + PnTender.COL_END_DATE + " >= :startDate ) OR("
                + PnTender.COL_START_DATE + " >= :startDate AND " + PnTender.COL_END_DATE + " <= :startDate ) OR("
                + PnTender.COL_START_DATE + " >= :startDate AND " + PnTender.COL_END_DATE + " <= :endDate ))";


        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":consolidateStatus", AttributeValue.builder().s(TenderDTO.StatusEnum.VALIDATED.getValue()).build());
        values.put(":startDate", AttributeValue.builder().s(startDate.toString()).build());
        values.put(":endDate", AttributeValue.builder().s(endDate.toString()).build());

        return this.getByFilter(conditional, PnTender.AUTHOR_INDEX, values, filter)
                .collectList()
                .flatMap(list -> {
                    if (list == null || list.isEmpty()){
                        return Mono.empty();
                    }
                    return Mono.just(list.get(0));
                });
    }
}
