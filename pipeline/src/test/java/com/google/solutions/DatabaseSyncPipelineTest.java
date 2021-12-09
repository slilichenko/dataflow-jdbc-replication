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

import com.google.solutions.DatabaseSyncPipeline.PipelineOutput;
import com.google.solutions.model.SyncJob;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.Field;
import org.apache.beam.sdk.schemas.Schema.FieldType;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.ValidatesRunner;
import org.apache.beam.sdk.values.Row;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DatabaseSyncPipelineTest extends BaseDataPersistentTest {

  private final Schema tableSchema = Schema.of(
      Field.of("a", FieldType.INT32),
      Field.of("b", FieldType.STRING),
      Field.of("ts", FieldType.DATETIME)
  );

  @Rule
  public TestPipeline p = TestPipeline.create();

  @Test
  @Category(ValidatesRunner.class)
  public void noDataIfNoUpdates() {
    Collection<SyncJob> syncInfo = Collections.singleton(tableSyncMetadata(60));

    PipelineOutput result = DatabaseSyncPipeline
        .readRecords(p, dataSourceConfiguration, syncInfo);

    PAssert.that(result.records.values().iterator().next()).empty();

    p.run().waitUntilFinish();
  }

  @Test
  @Category(ValidatesRunner.class)
  public void updatedAndInsertedRowsRetrieved() throws Exception {

    // Update a row
    Instant updateTimestamp = Instant.now().minus(Duration.ofMinutes(60));
    int key = 2;
    String newValue = "Twenty";
    updateRecord(key, newValue, updateTimestamp);

    // Insert a new row
    Data newRecord = new Data(30, "Thirty", Instant.now().minus(Duration.ofMinutes(45)));
    insert(newRecord);

    PipelineOutput result = DatabaseSyncPipeline.readRecords(p,
        dataSourceConfiguration, Collections.singleton(tableSyncMetadata(80)));

    Row expectedUpdated = Row.withSchema(tableSchema)
        .withFieldValue("a", key)
        .withFieldValue("b", newValue)
        .withFieldValue("ts", toSchemaDatetime(updateTimestamp))
        .build();
    Row expectedInserted = Row.withSchema(tableSchema)
        .withFieldValue("a", newRecord.a)
        .withFieldValue("b", newRecord.b)
        .withFieldValue("ts", toSchemaDatetime(newRecord.ts))
        .build();

    PAssert.that(result.records.values().iterator().next())
        .containsInAnyOrder(expectedUpdated, expectedInserted);

    p.run().waitUntilFinish();
  }

  private SyncJob tableSyncMetadata(int minutesBack) {
    return new SyncJob(
        tableName,
        Instant.now().minus(Duration.ofMinutes(minutesBack)),
        "select a, b, ts from " + tableName + " where ts between ? and ?");
  }

}
