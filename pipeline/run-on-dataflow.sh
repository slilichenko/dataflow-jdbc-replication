#!/usr/bin/env bash
set -u

export REGION=us-east1

TEMPLATE_PATH="gs://${METADATA_BUCKET}/jdbc-extract-template.json"

set +u
EXTRA_OPTIONS=""
if [[ ! -z "${DF_NETWORK}" ]]; then
  EXTRA_OPTIONS="${EXTRA_OPTIONS} --network=${DF_NETWORK}"
fi

if [[ ! -z "${DF_SUBNETWORK}" ]]; then
  EXTRA_OPTIONS="${EXTRA_OPTIONS} --subnetwork=${DF_SUBNETWORK}"
fi
set -u

TABLES_PARAM_FILE=tables.yaml
echo "--parameters:" > ${TABLES_PARAM_FILE}
echo "  tables: ${TABLE_LIST}" >> ${TABLES_PARAM_FILE}

JOB_ID="jdbc-export-`date +%Y%m%d-%H%M%S`"
set -x
gcloud dataflow flex-template run ${JOB_ID} \
    --template-file-gcs-location "${TEMPLATE_PATH}" \
    --region ${REGION} \
    --service-account-email ${DATAFLOW_WORKER_SA} \
    --staging-location gs://${DF_TEMP_BUCKET}/tmp \
    ${EXTRA_OPTIONS}  \
    --flags-file ${TABLES_PARAM_FILE} \
    --parameters GCSTableMetadataBucket=${METADATA_BUCKET} \
    --parameters outputFolder=gs://${OUTPUT_BUCKET} \
    --parameters "databaseConnectionURLSecretId=${DB_URL_SECRET_ID}"
set +x

rm ${TABLES_PARAM_FILE}


