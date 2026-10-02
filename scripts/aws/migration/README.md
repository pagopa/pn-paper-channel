
Installare jq per l'esecuzione dello script:

```
## Su Debian-like OS
sudo apt install jq
```  

Per caricare i dati in un ambiente:

```
./load-zones.sh [-p ${PROFILE}] -r ${REGION} -f ${FILE_PATH}
```  

Esempio DEV: 
```
./load-zones.sh -p profilo_dev_core -r eu-south-1 -f ./zones.csv
``` 

Esempio Localstack: 
```
./load-zones.sh -r us-east-1 -f ./zones.csv
```