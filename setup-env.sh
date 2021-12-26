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

export TF_VAR_project_id=${PROJECT_ID}
cd terraform
terraform init && terraform apply || echo "Failed to run the Terraform scripts"

# These variables will be used in several scripts to run the client app and the pipeline
export DB_URL_SECRET_ID=$(terraform output -raw jdbc-url-secret-id)
export DATAFLOW_WORKER_SA=$(terraform output -raw dataflow-worker-sa)
export METADATA_BUCKET=$(terraform output -raw metadata-bucket)
export OUTPUT_BUCKET=$(terraform output -raw output-bucket)
export DF_TEMP_BUCKET=$(terraform output -raw df-temp-bucket)
export PSQL_INSTANCE=$(terraform output -raw postgresql-instance)
export PSQL_DATABASE=$(terraform output -raw postgresql-database)
export PSQL_CONNECTION=$(terraform output -raw postgresql-connection-name)
export REGION=$(terraform output -raw region)
export REPO_NAME=$(terraform output -raw repo-name)
export REPO_LOCATION=$(terraform output -raw repo-location)
export REPO_PROJECT_ID=$(terraform output -raw repo-project-id)

# This step will create the PostgreSQL schema and tables.
cd ../client-app
mvn org.flywaydb:flyway-maven-plugin::migrate

cd ..

