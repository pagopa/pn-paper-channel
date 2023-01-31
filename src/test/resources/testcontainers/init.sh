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

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name CapDynamoTable \
    --attribute-definitions \
        AttributeName=cap,AttributeType=S \
    --key-schema \
        AttributeName=cap,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name DeliveryFileDynamoTable \
    --attribute-definitions \
        AttributeName=uuid,AttributeType=S \
    --key-schema \
        AttributeName=uuid,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "35031" }, "city": {"S": "Abano Terme"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "26834" }, "city": {"S": "Abbadia Cerreto"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "23821" }, "city": {"S": "Abbadia Lariana"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "53021" }, "city": {"S": "Abbadia San Salvatore"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "09071" }, "city": {"S": "Abbasanta"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "65020" }, "city": {"S": "Abbateggio"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "20081" }, "city": {"S": "Abbiategrasso"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "51024" }, "city": {"S": "Abetone Cutigliano"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "85010" }, "city": {"S": "Abriola"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "97011" }, "city": {"S": "Acate"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "71021" }, "city": {"S": "Accadia"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "12021" }, "city": {"S": "Acceglio"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "75011" }, "city": {"S": "Accettura"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "67020" }, "city": {"S": "Acciano"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "02011" }, "city": {"S": "Accumoli"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "85011" }, "city": {"S": "Acerenza"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "84042" }, "city": {"S": "Acerno"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "80011" }, "city": {"S": "Acerra"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "95020" }, "city": {"S": "Aci Bonaccorsi"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "95021" }, "city": {"S": "Aci Castello"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "95022" }, "city": {"S": "Aci Catena"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "95025" }, "city": {"S": "Aci Sant''Antonio"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "95024" }, "city": {"S": "Acireale"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "03040" }, "city": {"S": "Acquafondata"}}'
aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"cap": {"S": "03040" }, "city": {"S": "Acquafondata"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryFileDynamoTable  \
    --item '{"uuid": {"S": "12345" }, "status": {"S": "UPLOADING"}, "url": {"S": "www.abcd.it"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "CXJ564" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "GLS"}, "taxId": {"S": "1234957483"}, "phoneNumber": {"S": "351543654"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'


aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "CXJ664" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "NEXIVE"}, "taxId": {"S": "12312434324"}, "phoneNumber": {"S": "23432432234"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'


aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "LOP3222" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "BRT"}, "taxId": {"S": "21432432342"}, "phoneNumber": {"S": "32423455322"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CostDynamoTable \
    --item '{"idDeliveryDriver": {"S": "LOP3222"}, "tenderCode": {"S": "GARA-2022"}, "uuid": {"S": "abc12345"}, "cap": {"S": "99999"}, "productType": {"S": "AR"}, "basePrice":{"S": "30.00"}, "pagePrice": {"S": "10.00"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CostDynamoTable \
    --item '{"idDeliveryDriver": {"S": "LOP3222"}, "tenderCode": {"S": "GARA-2022"}, "uuid": {"S": "abc6789"}, "zone": {"S": "ZONE_1"}, "productType": {"S": "AR"}, "basePrice":{"S": "5.00"}, "pagePrice": {"S": "1.00"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name ZoneDynamoTable  \
    --item '{"zone": {"S": "ZONE_1" }, "countryEn": {"S": "Albania"}, "countryIt": {"S": "Albania"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name ZoneDynamoTable  \
    --item '{"zone": {"S": "ZONE_1" }, "countryEn": {"S": "Andorra"}, "countryIt": {"S": "Andorra"}}'

echo "Initialization terminated"