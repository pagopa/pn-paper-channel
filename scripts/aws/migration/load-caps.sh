tail -n +2 caps.csv | parallel --pipe -N25 'head -c -1 | jq --slurp --raw-input "{ \"pn-PaperCap\": split(\"\n\") | map(split(\",\")) |
    map(
		{
			\"PutRequest\": {
				\"Item\": {
					\"author\": {
						\"S\": \"PN-PAPER-CHANNEL\"
					},
					\"cap\": {
						\"S\": .[0]
					},
					\"city\": {
						\"S\": .[1]
					}
				}
			}
		}
	)
} 
" >split_caps_{#}.json'
for f in $(find . -maxdepth 1 -name "split_caps_*.json" -type f); do 
	sleep 3
	aws dynamodb batch-write-item   --profile $PROFILE --region $REGION  --endpoint-url=$ENDPOINT --request-items file://$f;
done
# rm -f split_caps_*.json
