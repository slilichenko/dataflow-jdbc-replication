#!/usr/bin/env bash
set -u

export TF_VAR_project_id=${PROJECT_ID}
cd terraform
terraform init && terraform apply

# These variables will be used in several scripts to run the client app and the pipeline
export DB_URL_SECRET_ID=$(terraform output -raw jdbc-url-secret-id)
export DATAFLOW_WORKER_SA=$(terraform output -raw dataflow-worker-sa)
export METADATA_BUCKET=$(terraform output -raw metadata-bucket)
export OUTPUT_BUCKET=$(terraform output -raw output-bucket)
export DF_TEMP_BUCKET=$(terraform output -raw df-temp-bucket)
export PSQL_INSTANCE=$(terraform output -raw postgresql_instance)
export PSQL_DATABASE=$(terraform output -raw postgresql_database)
export PSQL_CONNECTION=$(terraform output -raw postgresql_connection_name)

# This step will create the PostgreSQL schema and tables.
cd ../client-app
mvn org.flywaydb:flyway-maven-plugin::migrate

cd ..

