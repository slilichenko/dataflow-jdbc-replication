#!/usr/bin/env bash
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
