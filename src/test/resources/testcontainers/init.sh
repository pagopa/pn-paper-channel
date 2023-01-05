echo "### CREATE QUEUES ###"
queues="local-delivery-push-inputs local-delivery-push-safestorage-inputs local-delivery-push-actions local-ext-channels-inputs local-ext-channels-outputs local-delivery-push-actions-done local-ext-channels-elab-res local-user-attributes-actions"
for qn in  $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done

echo " - Create pn-paper-channel TABLES"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name RequestDeliveryDynamoTable \
    --attribute-definitions \
        AttributeName=requestId,AttributeType=S \
        AttributeName=fiscalCode,AttributeType=S \
    --key-schema \
        AttributeName=requestId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"fiscal-code-index\",
            \"KeySchema\": [{\"AttributeName\":\"fiscalCode\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name AddressDynamoTable  \
    --attribute-definitions \
        AttributeName=requestId,AttributeType=S \
    --key-schema \
        AttributeName=requestId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5





echo "Initialization terminated"