package it.pagopa.pn.paperchannel.middleware.db.dao.common;

import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
public abstract class BaseDAO<T> {

    @Getter
    @Setter
    @AllArgsConstructor
    protected static class Keys {
        Key from;
        Key to;
    }

    private DataEncryption dataEncryption;
    protected final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    protected final DynamoDbAsyncClient dynamoDbAsyncClient;
    protected final DynamoDbAsyncTable<T> dynamoTable;
    protected final String table;
    protected static final Function<Key, QueryConditional> CONDITION_EQUAL_TO = QueryConditional::keyEqualTo;
    protected static final Function<Key, QueryConditional> CONDITION_BEGINS_WITH = QueryConditional::sortBeginsWith;
    protected static final Function<BaseDAO.Keys, QueryConditional> CONDITION_BETWEEN = keys -> QueryConditional.sortBetween(keys.getFrom(), keys.getTo());

    private final Class<T> tClass;


    protected BaseDAO(DataEncryption dataEncryption, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient, String tableName, Class<T> tClass) {
        this.dynamoTable = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(tClass));
        this.table = tableName;
        this.dataEncryption = dataEncryption;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tClass = tClass;
    }

    protected BaseDAO(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient, String tableName, Class<T> tClass) {
        this.dynamoTable = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(tClass));
        this.table = tableName;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tClass = tClass;
    }

    protected CompletableFuture<T> put(T entity){
        PutItemEnhancedRequest<T> putRequest = PutItemEnhancedRequest.builder(tClass)
                .item(encode(entity))
                .build();
        return dynamoTable.putItem(putRequest).thenApply(x -> entity);
    }

    protected CompletableFuture<T> delete(String partitionKey, String sortKey){
        Key.Builder keyBuilder = Key.builder().partitionValue(partitionKey);
        if (!StringUtils.isBlank(sortKey)){
            keyBuilder.sortValue(sortKey);
        }
        return dynamoTable.deleteItem(keyBuilder.build());
    }

    protected CompletableFuture<Void> putWithTransact(TransactWriteItemsEnhancedRequest transactRequest){
        return dynamoDbEnhancedAsyncClient.transactWriteItems(transactRequest).thenApply(item -> null);
    }

    protected CompletableFuture<T> update(T entity){
        UpdateItemEnhancedRequest<T> updateRequest = UpdateItemEnhancedRequest
                .builder(tClass).item(entity).build();
        return dynamoTable.updateItem(updateRequest);
    }

    protected CompletableFuture<T> get(String partitionKey, String sortKey){
        Key.Builder keyBuilder = Key.builder().partitionValue(partitionKey);
        if (!StringUtils.isBlank(sortKey)){
            keyBuilder.sortValue(sortKey);
        }

        return dynamoTable.getItem(keyBuilder.build()).thenApply(this::decode);
    }

    protected Flux<T> getBySecondaryIndex(String index, String partitionKey, String sortKey){

        Key.Builder keyBuilder = Key.builder().partitionValue(partitionKey);
        if (!StringUtils.isBlank(sortKey)){
            keyBuilder.sortValue(sortKey);
        }

        return Flux.from(dynamoTable.index(index).query(QueryConditional.keyEqualTo(keyBuilder.build())).flatMapIterable(Page::items));
    }


    protected CompletableFuture<Integer> getCounterQuery(Map<String, AttributeValue> values, String filterExpression, String keyConditionExpression){
        QueryRequest.Builder qeRequest = QueryRequest
                .builder()
                .select(Select.COUNT)
                .tableName(table)
                .keyConditionExpression(keyConditionExpression)
                .expressionAttributeValues(values);

        if (!StringUtils.isBlank(filterExpression)){
            qeRequest.filterExpression(filterExpression);
        }

        return dynamoDbAsyncClient.query(qeRequest.build()).thenApply(QueryResponse::count);
    }

    protected Flux<T> getByFilter(QueryConditional conditional, String index, Map<String, AttributeValue> values, String filterExpression, Integer maxElements){
        QueryEnhancedRequest.Builder qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(conditional);
        if (maxElements != null) {
            qeRequest.limit(maxElements);
        }
        if (!StringUtils.isBlank(filterExpression)){
            qeRequest.filterExpression(Expression.builder().expression(filterExpression).expressionValues(values).build());
        }
        if (StringUtils.isNotBlank(index)){
            return Flux.from(dynamoTable.index(index).query(qeRequest.build()).flatMapIterable(Page::items));
        }
        return Flux.from(dynamoTable.query(qeRequest.build()).flatMapIterable(Page::items));
    }

    protected Flux<T> getByFilter(QueryConditional conditional, String index, Map<String, AttributeValue> values, String filterExpression){
        return getByFilter(conditional, index, values, filterExpression, null);
    }


    protected <A> A encode(A data) {
        if(data instanceof PnAddress pnAddress) {
            pnAddress.setFullName(dataEncryption.encode(pnAddress.getFullName()));
            pnAddress.setNameRow2(dataEncryption.encode(pnAddress.getNameRow2()));
            pnAddress.setAddress(dataEncryption.encode(pnAddress.getAddress()));
            pnAddress.setAddressRow2(dataEncryption.encode(pnAddress.getAddressRow2()));
            pnAddress.setCap(dataEncryption.encode(pnAddress.getCap()));
            pnAddress.setCity(dataEncryption.encode(pnAddress.getCity()));
            pnAddress.setCity2(dataEncryption.encode(pnAddress.getCity2()));
            pnAddress.setPr(dataEncryption.encode(pnAddress.getPr()));
            pnAddress.setCountry(dataEncryption.encode(pnAddress.getCountry()));
        }
        if(data instanceof PnDeliveryRequest pnDeliveryRequest) {
            pnDeliveryRequest.setFiscalCode(dataEncryption.encode(pnDeliveryRequest.getFiscalCode(), pnDeliveryRequest.getReceiverType()));
        }
        return data;
    }

    protected T decode(T data){
        if(data instanceof PnAddress pnAddress) {
            pnAddress.setFullName(dataEncryption.decode(pnAddress.getFullName()));
            pnAddress.setNameRow2(dataEncryption.decode(pnAddress.getNameRow2()));
            pnAddress.setAddress(dataEncryption.decode(pnAddress.getAddress()));
            pnAddress.setAddressRow2(dataEncryption.decode(pnAddress.getAddressRow2()));
            pnAddress.setCap(dataEncryption.decode(pnAddress.getCap()));
            pnAddress.setCity(dataEncryption.decode(pnAddress.getCity()));
            pnAddress.setCity2(dataEncryption.decode(pnAddress.getCity2()));
            pnAddress.setPr(dataEncryption.decode(pnAddress.getPr()));
            pnAddress.setCountry(dataEncryption.decode(pnAddress.getCountry()));
        }
        if(data instanceof PnDeliveryRequest pnDeliveryRequest) {
            pnDeliveryRequest.setFiscalCode(dataEncryption.decode(pnDeliveryRequest.getFiscalCode()));
        }
        return data;
    }

    protected Key keyBuild(String partitionKey, String sortKey){
        Key.Builder builder = Key.builder().partitionValue(partitionKey);
        if (StringUtils.isNotBlank(sortKey)){
            builder.sortValue(sortKey);
        }
        return builder.build();
    }

    protected Flux<T> findAllByKeys(String partitionKey, String... sortKeys) {
        ReadBatch.Builder<T> builder = ReadBatch.builder(tClass)
                .mappedTableResource(this.dynamoTable);

        for(String sortKey: sortKeys ) {
            Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
            builder.addGetItem(key);
        }

        BatchGetResultPagePublisher batchGetResultPagePublisher = dynamoDbEnhancedAsyncClient.batchGetItem(BatchGetItemEnhancedRequest.builder()
                .readBatches(builder.build())
                .build());

        return Mono.from(batchGetResultPagePublisher.map(batchGetResultPage -> batchGetResultPage.resultsForTable(this.dynamoTable)))
                .flatMapMany(Flux::fromIterable);
    }

    protected Mono<Void> deleteBatch(String partitionKey, String... sortKeys) {
        WriteBatch.Builder<T> builder = WriteBatch.builder(tClass)
                .mappedTableResource(this.dynamoTable);

        for (String sortKey : sortKeys) {
            Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
            builder.addDeleteItem(key);
        }

        CompletableFuture<BatchWriteResult> batchWriteResultCompletableFuture = dynamoDbEnhancedAsyncClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .addWriteBatch(builder.build())
                .build());


        return Mono.fromFuture(batchWriteResultCompletableFuture).then();
    }

}
