tail -n +2 zones.csv | parallel --pipe -N25 'head -c -1 | jq --slurp --raw-input "{ \"pn-PaperZone\": split(\"\n\") | map(split(\",\")) |
    map(
		{
			\"PutRequest\": {
				\"Item\": {
					\"zone\": {
						\"S\": .[0]
					},
					\"countryIt\": {
						\"S\": .[1]
					},
					\"countryEn\": {
						\"S\": .[2]
					}
				}
			}
		}
	)
} 
" >split_zones_{#}.json; aws dynamodb batch-write-item   --profile $PROFILE --region $REGION  --endpoint-url=$ENDPOINT --request-items file://split_zones_{#}.json'
rm -f split_zones_*.json
