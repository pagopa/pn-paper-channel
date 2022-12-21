package it.pagopa.pn.paperchannel.middleware.db.dao.common;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


public abstract class BaseDAO<T> {

    protected final PnAuditLogBuilder auditLogBuilder;
    private final KmsEncryption kmsEncryption;
    protected final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    protected final DynamoDbAsyncClient dynamoDbAsyncClient;
    protected final DynamoDbAsyncTable<T> dynamoTable;
    protected final String table;
    private final Class<T> tClass;


    protected BaseDAO(PnAuditLogBuilder auditLogBuilder, KmsEncryption kmsEncryption, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient, String tableName, Class<T> tClass) {
        this.dynamoTable = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(tClass));
        this.table = tableName;
        this.auditLogBuilder = auditLogBuilder;
        this.kmsEncryption = kmsEncryption;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tClass = tClass;
    }

    protected CompletableFuture<T> put(T entity){
        PutItemEnhancedRequest<T> putRequest = PutItemEnhancedRequest.builder(tClass)
                .item(encode(entity, this.tClass))
                .build();
        return dynamoTable.putItem(putRequest).thenApply(x -> entity);
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

        return decode(dynamoTable.getItem(keyBuilder.build()));
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

    protected <A> A encode(A data, Class<A> aClass) {
        if(aClass == PnAddress.class) {
            ((PnAddress) data).setFullName(kmsEncryption.encode(((PnAddress) data).getFullName()));
            ((PnAddress) data).setNameRow2(kmsEncryption.encode(((PnAddress) data).getNameRow2()));
            ((PnAddress) data).setAddress(kmsEncryption.encode(((PnAddress) data).getAddress()));
            ((PnAddress) data).setAddressRow2(kmsEncryption.encode(((PnAddress) data).getAddressRow2()));
            ((PnAddress) data).setCap(kmsEncryption.encode(((PnAddress) data).getCap()));
            ((PnAddress) data).setCity(kmsEncryption.encode(((PnAddress) data).getCity()));
            ((PnAddress) data).setCity2(kmsEncryption.encode(((PnAddress) data).getCity2()));
            ((PnAddress) data).setPr(kmsEncryption.encode(((PnAddress) data).getPr()));
            ((PnAddress) data).setCountry(kmsEncryption.encode(((PnAddress) data).getCountry()));
        }
        return data;
    }

    protected CompletableFuture<T> decode(CompletableFuture<T> genericData) {
        return genericData.thenApply(data -> {
            if(data instanceof PnAddress) {
                ((PnAddress) data).setFullName(kmsEncryption.decode(((PnAddress) data).getFullName()));
                ((PnAddress) data).setNameRow2(kmsEncryption.decode(((PnAddress) data).getNameRow2()));
                ((PnAddress) data).setAddress(kmsEncryption.decode(((PnAddress) data).getAddress()));
                ((PnAddress) data).setAddressRow2(kmsEncryption.decode(((PnAddress) data).getAddressRow2()));
                ((PnAddress) data).setCap(kmsEncryption.decode(((PnAddress) data).getCap()));
                ((PnAddress) data).setCity(kmsEncryption.decode(((PnAddress) data).getCity()));
                ((PnAddress) data).setCity2(kmsEncryption.decode(((PnAddress) data).getCity2()));
                ((PnAddress) data).setPr(kmsEncryption.decode(((PnAddress) data).getPr()));
                ((PnAddress) data).setCountry(kmsEncryption.decode(((PnAddress) data).getCountry()));
            }
            return data;
        });
    }
}
