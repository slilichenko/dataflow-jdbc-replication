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
set -e

# See stage-dataflow-template.sh for details on template naming conventions.
TEMPLATE_NAME=$(gsutil ls gs://${METADATA_BUCKET}/jdbc-extract-template-*.json | sort -r | head -n 1)

if [ -z "${TEMPLATE_NAME}" ]
then
      echo "Can't find any template in gs://${METADATA_BUCKET}"
else
      ./run-on-dataflow.sh ${TEMPLATE_NAME}
fi