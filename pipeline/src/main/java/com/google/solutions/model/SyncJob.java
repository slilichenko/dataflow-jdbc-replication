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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.joda.time.format.DateTimeFormat;

@DefaultCoder(SerializableCoder.class)
public class SyncJob implements Serializable {

  private final static long serialVersionUID = 1L;

  @JsonIgnore
  private String name;
  private Instant lastSync;
  @JsonIgnore
  private Instant previousSync;
  private String query;

  private SyncJob() {
    // For Jackson's use
  }

  public SyncJob(String name, Instant lastSync, String query) {
    this.name = name;
    this.lastSync = lastSync;
    this.query = query;
  }

  public static SyncJob fromJsonBytes(String name, byte[] content) throws IOException {

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    SyncJob syncJob = objectMapper.readValue(content, SyncJob.class);
    syncJob.name = name;
    return syncJob;

  }

  public SyncJob nextSyncPoint(Instant newSyncPoint) {
    SyncJob result = new SyncJob(this.name, newSyncPoint, this.query);
    result.previousSync = this.getLastSync();
    return result;
  }

  public String getName() {
    return name;
  }

  public Instant getLastSync() {
    return lastSync;
  }

  public Instant getPreviousSync() {
    return previousSync;
  }

  public String getQuery() {
    return query;
  }

  public byte[] toJsonBytes() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    objectMapper.writeValue(byteArrayOutputStream, this);
    return byteArrayOutputStream.toByteArray();
  }

  private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
      .withZone(ZoneId.of("UTC"));

  @JsonIgnore
  public String getOutputFileNamePrefix() {
    if (previousSync == null) {
      throw new IllegalStateException("Previous sync date is null for job " + name);
    }

    return name + '_' + timeFormatter.format(previousSync) + "_" + timeFormatter.format(lastSync);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SyncJob syncJob = (SyncJob) o;

    if (name != null ? !name.equals(syncJob.name) : syncJob.name != null) {
      return false;
    }
    if (lastSync != null ? !lastSync.equals(syncJob.lastSync) : syncJob.lastSync != null) {
      return false;
    }
    return query != null ? query.equals(syncJob.query) : syncJob.query == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (lastSync != null ? lastSync.hashCode() : 0);
    result = 31 * result + (query != null ? query.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SyncJob{" +
        "name='" + name + '\'' +
        ", lastSync=" + lastSync +
        ", query='" + query + '\'' +
        '}';
  }
}
