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

package com.google.solutions.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Instant;
import org.junit.Test;

public class SyncJobTest {

  @Test
  public void testSerializationToJson() throws IOException {
    SyncJob data = new SyncJob("jobName", Instant.now(), "Select 1");
    byte[] serialized = data.toJsonBytes();

    SyncJob deserialized = SyncJob.fromJsonBytes("jobName", serialized);
    assertEquals("Original and deserialized are equal", data, deserialized);
    assertEquals("Names match", "jobName", deserialized.getName());
  }

  @Test
  public void testGetOutputFileNamePrefix() {
    SyncJob previous = new SyncJob("test", Instant.ofEpochSecond(0), "Select 1");
    SyncJob next = previous.nextSyncPoint(Instant.ofEpochSecond(1638986912));

    assertEquals("test_19700101-000000_20211208-180832", next.getOutputFileNamePrefix());
  }
}