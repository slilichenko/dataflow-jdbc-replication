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

public class TableSyncMetadataTest {

  @Test
  public void testSerializationToJson() throws IOException {
    TableSyncMetadata data = new TableSyncMetadata("table", Instant.now(), "Select 1");
    byte[] serialized = data.toJsonBytes();

    TableSyncMetadata deserialized = TableSyncMetadata.fromJsonBytes(serialized);
    assertEquals("Original and deserialized are equal", data, deserialized);
  }

}