#!/usr/bin/env bash
#
# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -u

TEMPLATE_PATH=$1

set +u
EXTRA_OPTIONS=""
if [[ ! -z "${DF_NETWORK}" ]]; then
  EXTRA_OPTIONS="${EXTRA_OPTIONS} --network=${DF_NETWORK}"
fi

if [[ ! -z "${DF_SUBNETWORK}" ]]; then
  EXTRA_OPTIONS="${EXTRA_OPTIONS} --subnetwork=${DF_SUBNETWORK}"
fi
set -u

JOBS_PARAM_FILE=jobs.yaml
echo "--parameters:" > ${JOBS_PARAM_FILE}
echo "  jobNames: ${JOB_LIST}" >> ${JOBS_PARAM_FILE}

JOB_ID="jdbc-export-`date +%Y%m%d-%H%M%S`"
set -x
gcloud dataflow flex-template run ${JOB_ID} \
    --template-file-gcs-location "${TEMPLATE_PATH}" \
    --region ${REGION} \
    --service-account-email ${DATAFLOW_WORKER_SA} \
    --staging-location gs://${DF_TEMP_BUCKET}/tmp \
    ${EXTRA_OPTIONS}  \
    --flags-file ${JOBS_PARAM_FILE} \
    --parameters GCSTableMetadataBucket=${METADATA_BUCKET} \
    --parameters outputFolder=gs://${OUTPUT_BUCKET} \
    --parameters "databaseConnectionURLSecretId=${DB_URL_SECRET_ID}"
set +x

rm ${JOBS_PARAM_FILE}


