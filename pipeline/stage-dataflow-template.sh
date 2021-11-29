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

set -e
set -u

mvn clean package

export TEMPLATE_IMAGE="gcr.io/${PROJECT_ID}/dataflow/jdbc-extract:latest"
export TEMPLATE_PATH="gs://${METADATA_BUCKET}/jdbc-extract-template.json"

echo "Deploying dataflow template to: ${TEMPLATE_IMAGE}"

JAR_LIST="--jar target/jdbc-pipeline-1.0-SNAPSHOT.jar"
for d in target/dependencies/* ;
do
  JAR_LIST="${JAR_LIST} --jar $d"
done

gcloud dataflow flex-template build ${TEMPLATE_PATH} \
  --image-gcr-path "${TEMPLATE_IMAGE}" \
  --sdk-language "JAVA" \
  --flex-template-base-image JAVA11 \
  --metadata-file "pipeline-template-metadata.json" \
  ${JAR_LIST} \
  --env FLEX_TEMPLATE_JAVA_MAIN_CLASS="com.google.solutions.DatabaseSyncPipeline"
