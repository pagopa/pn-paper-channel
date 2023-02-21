
Installare jq per l'esecuzione dello script:

```
## Su Debian-like OS
sudo apt install jq
```  

Per caricare i dati in un ambiente:

```
./load-caps.sh [-p ${PROFILE}] -r ${REGION} -f ${FILE_PATH}
./load-zones.sh [-p ${PROFILE}] -r ${REGION} -f ${FILE_PATH}
```  

Esempio DEV: 
```
./load-caps.sh -p profilo_dev_core -r eu-south-1 -f ./caps.csv
./load-zones.sh -p profilo_dev_core -r eu-south-1 -f ./zones.csv
``` 

Esempio Localstack: 
```
./load-caps.sh -r us-east-1 -f ./caps.csv
./load-zones.sh -r us-east-1 -f ./zones.csv
``` 

Utilizzare il comando seguente per convertire il file dei CAP da una struttura flat (in cui più città possono avere lo stesso CAP) alla struttura per il caricamento (città raggruppate per CAP)

```
awk -F, '{if(a[$1])a[$1]=a[$1]"/"$2; else a[$1]=$2;}END{for (i in a)print i, a[i];}' OFS=, caps-flat.csv > caps.csv
```