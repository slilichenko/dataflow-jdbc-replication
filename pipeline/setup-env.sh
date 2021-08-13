#!/usr/bin/env bash
set -u

export TF_VAR_project_id=${PROJECT_ID}
cd ../terraform
terraform init && terraform apply

# Needed by stage-dataflow-template.sh and run-on-dataflow.sh
export DB_URL_SECRET_ID=$(terraform output database-url-secret-id)
export DATAFLOW_WORKER_SA=$(terraform output dataflow-worker-sa)
export METADATA_BUCKET=$(terraform output metadata-bucket)
export OUTPUT_BUCKET=$(terraform output output-bucket)
export DF_TEMP_BUCKET=$(terraform output df-temp-bucket)

cd ../pipeline

