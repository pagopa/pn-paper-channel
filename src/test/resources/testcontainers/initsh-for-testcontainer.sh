
bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-paper-channel/9e428ac93f7a221977446a29525f68534ead439a/src/test/resources/testcontainers/init.sh)

echo "### CREATE QUEUES ###"
queues="local-delivery-push-safestorage-inputs local-delivery-push-actions local-ext-channels-inputs local-ext-channels-outputs local-delivery-push-actions-done local-ext-channels-elab-res local-user-attributes-actions pn-f24_to_paperchannel"
for qn in  $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done
