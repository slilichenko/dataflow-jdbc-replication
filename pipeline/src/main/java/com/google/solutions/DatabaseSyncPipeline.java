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

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.solutions.model.TableSyncMetadata;
import com.sap.db.jdbc.Driver;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.VoidCoder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.WriteFilesResult;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.io.jdbc.JdbcIO.StatementPreparator;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.ToJson;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatabaseSyncPipeline {

  public static final int SYNC_UP_TO_X_MINUTES = 3;
  private final static Logger LOG = LoggerFactory.getLogger(DatabaseSyncPipeline.class);
  private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  public static void main(String[] args) throws IOException {
    DatabaseSyncPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args).as(DatabaseSyncPipelineOptions.class);

    String databaseUrl = getDatabaseConnectionURL(options.getDatabaseConnectionURLSecretId());
    DataSourceConfiguration dataSourceConfiguration = DataSourceConfiguration
        .create(Driver.class.getName(), databaseUrl);

    List<String> tables = options.getTables();
    String gcsTableMetadataFolder = options.getGCSTableMetadataBucket();

    Collection<TableSyncMetadata> syncInfo = readSyncInfo(gcsTableMetadataFolder, tables);

    Pipeline p = Pipeline.create(options);

    PipelineOutput result = readRecords(p, dataSourceConfiguration, syncInfo);

    persist(result, options.getOutputFolder(), options.getGCSTableMetadataBucket());
    p.run();
  }

  private static String getDatabaseConnectionURL(String secretId) throws IOException {
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      AccessSecretVersionResponse response = client.accessSecretVersion(secretId);

      return response.getPayload().getData().toStringUtf8();
    }
  }

  private static Collection<TableSyncMetadata> readSyncInfo(String gcsTableMetadataFolder,
      List<String> tables) {
    return tables.stream().map(table -> {
      Storage storage = StorageOptions.getDefaultInstance().getService();
      Bucket metadataBucket = storage.get(gcsTableMetadataFolder);
      Blob blob = metadataBucket.get(table + ".json");
      if (blob == null) {
        throw new IllegalStateException(
            "Sync metadata for table " + table + " is not found in GCS folder "
                + gcsTableMetadataFolder);
      }
      try {
        return TableSyncMetadata.fromJsonBytes(blob.getContent());
      } catch (IOException e) {
        throw new IllegalStateException("Sync metadata for table " + table + " is not valid JSON: ",
            e);
      }
    }).collect(Collectors.toUnmodifiableSet());
  }

  static PipelineOutput readRecords(Pipeline p, DataSourceConfiguration dataSourceConfiguration,
      Collection<TableSyncMetadata> syncInfos) {
    PipelineOutput result = new PipelineOutput();
    syncInfos.forEach(
        tableSyncMetadata -> {

          Instant syncedUpTo = tableSyncMetadata.getLastSync();
          Instant newSyncPoint = Instant.now().minus(Duration.ofMinutes(SYNC_UP_TO_X_MINUTES));
          if (syncedUpTo.isAfter(newSyncPoint)) {
            LOG.info("Table " + tableSyncMetadata.getTableName()
                + " has already been synced recently. Skipping.");
            return;
          }
          Counter counter = Metrics.counter("table-row-retriever", "row-counter");
          PCollection<Row> rows = p.apply("Read table data: " + tableSyncMetadata.getTableName(),
              JdbcIO.readRows()
                  .withQuery(tableSyncMetadata.getQuery())
                  .withStatementPreparator((StatementPreparator) ps -> {
                    ps.setTimestamp(1, Timestamp.from(syncedUpTo), UTC_CALENDAR);
                    ps.setTimestamp(2, Timestamp.from(newSyncPoint), UTC_CALENDAR);
                  })
                  .withOutputParallelization(false)
                  .withDataSourceConfiguration(dataSourceConfiguration));

          rows.apply("Capture metrics", MapElements.into(TypeDescriptors.voids()).via(
              row -> {
                counter.inc();
                return (Void) null;
              })).setCoder(VoidCoder.of());

          result.records.put(tableSyncMetadata.nextSyncPoint(newSyncPoint), rows);
        }
    );
    return result;
  }

  public static void persist(PipelineOutput result, String outputFolder, String metadataFolder) {
    result.records.forEach(
        (syncInfo, rows) -> {
          WriteFilesResult<Void> writeFilesResult = rows
              .apply("Convert to Json", ToJson.of())
              .apply("Persist to GCS", TextIO.<String>writeCustomType()
                  .to(outputFolder + "/"
                      + syncInfo.getTableName() + '_'
                      + syncInfo.getLastSync()
                  )
                  .withFormatFunction(
                      (SerializableFunction<String, String>) input -> input
                  )
                  .withoutSharding()
              );

          PCollection<KV<Void, String>> filenames = writeFilesResult
              .getPerDestinationOutputFilenames();
          filenames.apply("Update sync point for " + syncInfo.getTableName(),
              ParDo.of(new DoFn<KV<Void, String>, Void>() {
                @ProcessElement
                public void process(@Element KV<Void, String> e) {
                  LOG.info(
                      "Finished processing extract for table " + syncInfo.getTableName() + ": " + e
                          .getValue());
                  byte[] content;
                  try {
                    content = syncInfo.toJsonBytes();
                  } catch (IOException ioException) {
                    LOG.error("Failed to serialize sync info: " + syncInfo);
                    return;
                  }
                  Storage storage = StorageOptions.getDefaultInstance().getService();
                  Bucket metadataBucket = storage.get(metadataFolder);
                  metadataBucket.create(syncInfo.getTableName() + ".json",
                      content, "application/json");
                }

              }));
        }
    );
  }

  static class PipelineOutput {

    Map<TableSyncMetadata, PCollection<Row>> records = new HashMap<>();
  }
}
