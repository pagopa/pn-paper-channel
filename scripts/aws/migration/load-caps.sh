#! /bin/bash 

set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  # script cleanup here
}

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)


usage() {
      cat <<EOF
    Usage: $(basename "${BASH_SOURCE[0]}") [-h] [-v] [-p <aws-profile>] -r <aws-region> -f <csv-file>

    [-h]                      : this help message
    [-v]                      : verbose mode
    [-p <aws-profile>]        : aws cli profile (optional)
    -r <aws-region>           : aws region as eu-south-1
    -f <csv-file>             : csv file path
    
EOF
  exit 1
}

parse_params() {
  # default values of variables set from params
  aws_profile=""
  aws_region=""
  csv_file=""

  while :; do
    case "${1-}" in
    -h | --help) usage ;;
    -v | --verbose) set -x ;;
    -p | --profile) 
      aws_profile="${2-}"
      shift
      ;;
    -r | --region) 
      aws_region="${2-}"
      shift
      ;;
    -f | --file) 
      csv_file="${2-}"
      shift
      ;;
    -?*) die "Unknown option: $1" ;;
    *) break ;;
    esac
    shift
  done

  args=("$@")

  # check required params and arguments
  [[ -z "${aws_region-}" ]] && usage
  [[ -z "${csv_file-}" ]] && usage
  return 0
}

dump_params(){
  echo ""
  echo "######      PARAMETERS      ######"
  echo "##################################"
  echo "AWS region:         ${aws_region}"
  echo "AWS profile:        ${aws_profile}"
  echo "CSV file:        	${csv_file}"
}


# START SCRIPT

parse_params "$@"
dump_params

echo ""
echo "=== Base AWS command parameters"
aws_command_base_args=""
if ( [ ! -z "${aws_profile}" ] ) then
  aws_command_base_args="${aws_command_base_args} --profile $aws_profile"
fi
if ( [ ! -z "${aws_region}" ] ) then
  aws_command_base_args="${aws_command_base_args} --region  $aws_region"
fi
echo ${aws_command_base_args}

file=caps.csv

lines=$( cat $csv_file | wc -l )

i=2
while [ $i -lt $lines ]
do
  next_i=$(($i+25))
  echo "From $i to $next_i"

  tail -n +1 $csv_file \
      | sed -e 's/\([^,]*\),\(.*\)/ { "cap":"\1", "city":"\2"} /' \
      | jq -cs ".[$i:$next_i] " | jq 'map({
        "PutRequest": {
          "Item": {
            "author": { "S": "PN-PAPER-CHANNEL" }, 
            "cap": { "S": .cap }, 
            "city": { "S": .city }
          }
        }
      }) | { "pn-PaperCap": .} ' > _tmp_data.json
  
  aws ${aws_command_base_args} \
     dynamodb batch-write-item \
     --request-items file://_tmp_data.json

  i=$next_i
  
  sleep 1
done

rm _tmp_data.json

