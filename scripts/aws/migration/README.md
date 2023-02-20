
Installare jq e parallel per l'esecuzione dello script:

```
sudo apt install jq parallel
```  

Per caricare i dati in DEV:

```
export PROFILE="dev"  
export REGION="eu-south-1"  
export ENDPOINT="http://dynamodb.eu-south-1.amazonaws.com"  
./load-caps.sh  
./load-zones.sh
```  

Per caricare i dati su localstack:

```
export PROFILE="default"  
export REGION="us-east-1"  
export ENDPOINT="http://localstack:4566"  
./load-caps.sh  
./load-zones.sh
```  

Utilizzare il comando seguente per convertire il file dei CAP da una struttura flat (in cui più città possono avere lo stesso CAP) alla struttura per il caricamento (città raggruppate per CAP)

```
awk -F, '{if(a[$1])a[$1]=a[$1]"/"$2; else a[$1]=$2;}END{for (i in a)print i, a[i];}' OFS=, caps-flat.csv > caps.csv
```