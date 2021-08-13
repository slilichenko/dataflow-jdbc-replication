/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.solutions;

import java.util.List;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation.Required;

public interface DatabaseSyncPipelineOptions extends DataflowPipelineOptions {

  @Description("List of tables to update")
  @Required
  List<String> getTables();

  void setTables(List<String> value);

  @Description("Database Connection URL secret id.")
  @Required
  String getDatabaseConnectionURLSecretId();

  void setDatabaseConnectionURLSecretId(String value);

  @Description("Bucket that contains table metadata (without gs:// prefix)")
  @Required
  String getGCSTableMetadataBucket();

  void setGCSTableMetadataBucket(String value);

  @Description("GCS folder to create output files.")
  @Required
  String getOutputFolder();

  void setOutputFolder(String value);
}
