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
import com.google.solutions.model.SyncJob;
import com.google.solutions.util.SchemaUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import javax.sql.DataSource;
import org.apache.avro.generic.GenericRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.AvroGenericCoder;
import org.apache.beam.sdk.coders.VoidCoder;
import org.apache.beam.sdk.io.AvroIO;
import org.apache.beam.sdk.io.WriteFilesResult;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.io.jdbc.JdbcIO.StatementPreparator;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.utils.AvroUtils;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
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
        PipelineOptionsFactory.fromArgs(args).withValidation()
            .as(DatabaseSyncPipelineOptions.class);

    String databaseUrl = getDatabaseConnectionURL(options.getDatabaseConnectionURLSecretId());
    DataSourceConfiguration dataSourceConfiguration = DataSourceConfiguration
        .create(options.getJDBCDriverClassName(), databaseUrl);

    List<String> jobNames = options.getJobNames();
    String gcsTableMetadataFolder = options.getGCSTableMetadataBucket();

    Collection<SyncJob> syncJobs = readSyncInfo(gcsTableMetadataFolder, jobNames);

    Pipeline p = Pipeline.create(options);

    PipelineOutput result = readRecords(p, dataSourceConfiguration, syncJobs);

    persist(result, options.getOutputFolder(), options.getGCSTableMetadataBucket());

    p.run();
  }

  private static String getDatabaseConnectionURL(String secretId) throws IOException {
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      AccessSecretVersionResponse response = client.accessSecretVersion(secretId);

      return response.getPayload().getData().toStringUtf8();
    }
  }

  private static Collection<SyncJob> readSyncInfo(final String gcsTableMetadataFolder,
      final List<String> jobNames) {
    final Storage storage = StorageOptions.getDefaultInstance().getService();
    final Bucket metadataBucket = storage.get(gcsTableMetadataFolder);

    return jobNames.stream().map(jobName -> {
      Blob blob = metadataBucket.get(jobName + ".json");
      if (blob == null) {
        throw new IllegalStateException(
            "Sync metadata for job " + jobName + " is not found in GCS folder "
                + gcsTableMetadataFolder);
      }
      try {
        return SyncJob.fromJsonBytes(jobName, blob.getContent());
      } catch (IOException e) {
        throw new IllegalStateException(
            "Sync metadata for table " + jobName + " is not valid JSON: ",
            e);
      }
    }).collect(Collectors.toUnmodifiableSet());
  }

  static PipelineOutput readRecords(Pipeline p, DataSourceConfiguration dataSourceConfiguration,
      Collection<SyncJob> syncJobs) {
    PipelineOutput result = new PipelineOutput();

    DataSource dataSource = createDataSource(dataSourceConfiguration);

    final Instant newSyncPoint = Instant.now().minus(Duration.ofMinutes(SYNC_UP_TO_X_MINUTES));

    syncJobs.forEach(
        syncJob -> {
          Instant syncedUpTo = syncJob.getLastSync();

          if (syncedUpTo.isAfter(newSyncPoint)) {
            LOG.info("Job " + syncJob.getName() + " has already been synced recently. Skipping.");
            return;
          }

          Schema schema = extractBeamSchema(dataSource, syncJob.getQuery());

          Counter counter = Metrics
              .counter("table-row-retriever", "record-counter-" + syncJob.getName());
          PCollection<Row> rows = p.apply("Run " + syncJob.getName(),
              JdbcIO.readRows()
                  .withQuery(syncJob.getQuery())
                  .withStatementPreparator((StatementPreparator) ps -> {
                    ps.setTimestamp(1, Timestamp.from(syncedUpTo), UTC_CALENDAR);
                    ps.setTimestamp(2, Timestamp.from(newSyncPoint), UTC_CALENDAR);
                  })
                  .withOutputParallelization(false)
                  .withDataSourceConfiguration(dataSourceConfiguration));

          rows.apply("Capture Metrics " + syncJob.getName(), MapElements.into(TypeDescriptors.voids()).via(
              row -> {
                counter.inc();
                return null;
              })).setCoder(VoidCoder.of());

          SyncJob updatedSyncJob = syncJob.nextSyncPoint(newSyncPoint);
          result.records.put(updatedSyncJob, rows);
          result.schemas.put(updatedSyncJob, schema);
        }
    );

    return result;
  }

  private static DataSource createDataSource(DataSourceConfiguration dataSourceConfiguration) {
    return JdbcIO.DataSourceProviderFromDataSourceConfiguration.of(dataSourceConfiguration)
        .apply(null);
  }

  private static Schema extractBeamSchema(
      DataSource dataSource,
      String query) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement statement =
            conn.prepareStatement(
                query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      return SchemaUtil.toBeamSchema(statement.getMetaData());
    } catch (SQLException e) {
      throw new RuntimeException("Failed to infer Beam schema for query: " + query, e);
    }
  }

  public static void persist(PipelineOutput result, String outputFolder, String metadataFolder) {
    result.records.forEach(
        (syncInfo, rows) -> {
          org.apache.avro.Schema avroSchema = AvroUtils.toAvroSchema(result.schemas.get(syncInfo));

          WriteFilesResult<Void> writeFilesResult = rows
              .apply("To GenericRecord", ParDo.of(new DoFn<Row, GenericRecord>() {
                @ProcessElement
                public void process(@Element Row row, OutputReceiver<GenericRecord> out) {
                  out.output(AvroUtils.toGenericRecord(row));
                }
              })).setCoder(AvroCoder.of(avroSchema))
              .apply("Persist to GCS", AvroIO.writeGenericRecords(avroSchema)
                  .to(outputFolder + "/" + syncInfo.getOutputFileNamePrefix() + ".avro")
                  .withoutSharding()
                  .withOutputFilenames()
              );

          PCollection<KV<Void, String>> filenames = writeFilesResult
              .getPerDestinationOutputFilenames();
          filenames.apply("Update sync data " + syncInfo.getName(),
              ParDo.of(new DoFn<KV<Void, String>, Void>() {
                @ProcessElement
                public void process(@Element KV<Void, String> e) {
                  LOG.info(
                      "Finished processing extract for job " + syncInfo.getName() + ": " + e
                          .getValue());
                  byte[] content;
                  try {
                    content = syncInfo.toJsonBytes();
                  } catch (IOException ioException) {
                    LOG.error("Failed to serialize sync info: " + syncInfo, ioException);
                    return;
                  }
                  Storage storage = StorageOptions.getDefaultInstance().getService();
                  Bucket metadataBucket = storage.get(metadataFolder);
                  metadataBucket.create(syncInfo.getName() + ".json",
                      content, "application/json");
                }

              }));
        }
    );
  }

  static class PipelineOutput {

    Map<SyncJob, PCollection<Row>> records = new HashMap<>();
    Map<SyncJob, Schema> schemas = new HashMap<>();
  }
}
