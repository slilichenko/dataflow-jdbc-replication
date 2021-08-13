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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

@DefaultCoder(SerializableCoder.class)
public class TableSyncMetadata implements Serializable {

  private final static long serialVersionUID = 1L;

  private String tableName;
  private Instant lastSync;
  private String query;

  private TableSyncMetadata() {
    // For Jackson's use
  }

  public TableSyncMetadata(String tableName, Instant lastSync, String query) {
    this.tableName = tableName;
    this.lastSync = lastSync;
    this.query = query;
  }

  public static TableSyncMetadata fromJsonBytes(byte[] content) throws IOException {

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    return objectMapper.readValue(content, TableSyncMetadata.class);

  }

  public TableSyncMetadata nextSyncPoint(Instant newSyncPoint) {
    return new TableSyncMetadata(this.getTableName(), newSyncPoint, this.getQuery());
  }

  public String getTableName() {
    return tableName;
  }

  public Instant getLastSync() {
    return lastSync;
  }

  public String getQuery() {
    return query;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableSyncMetadata that = (TableSyncMetadata) o;
    return tableName.equals(that.tableName) &&
        lastSync.equals(that.lastSync) &&
        query.equals(that.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, lastSync, query);
  }

  @Override
  public String toString() {
    return "TableSyncMetadata{" +
        "tableName='" + tableName + '\'' +
        ", lastSync=" + lastSync +
        ", query='" + query + '\'' +
        '}';
  }

  public byte[] toJsonBytes() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    objectMapper.writeValue(byteArrayOutputStream, this);
    return byteArrayOutputStream.toByteArray();
  }
}
