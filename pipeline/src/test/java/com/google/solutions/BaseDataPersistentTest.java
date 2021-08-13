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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.h2.Driver;
import org.junit.After;
import org.junit.Before;

abstract public class BaseDataPersistentTest {

  public static final String SAP_EMULATOR = "sap_emulator";
  public static final String JDBC_CONNECTION_URL = "jdbc:h2:mem:"
      + SAP_EMULATOR
      + ";DB_CLOSE_DELAY=-1";
  protected final String tableName = "table1";
  protected final JdbcIO.DataSourceConfiguration dataSourceConfiguration = DataSourceConfiguration
      .create(
          Driver.class.getName(),
          JDBC_CONNECTION_URL
      );
  private final Instant severalHoursBack = Instant.now().minus(Duration.ofMinutes(120));
  Data[] existingData = new Data[]{
      new Data(1, "one", severalHoursBack),
      new Data(2, "two", severalHoursBack),
      new Data(3, "three", severalHoursBack),
      new Data(4, "four", severalHoursBack),
      new Data(5, "five", severalHoursBack),
      new Data(6, "six", severalHoursBack),
      new Data(7, "seven", severalHoursBack)
  };

  @Before
  public void createDb() throws SQLException {
    Driver myDriver = new Driver();
    DriverManager.registerDriver(myDriver);

    try (Connection conn = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
      conn.setAutoCommit(true);
      Statement statement = conn.createStatement();
      statement.execute("CREATE TABLE table1 ("
          + "a int, "
          + "b varchar(20),"
          + "ts timestamp "
          + ")");
    }
    for (Data record : existingData) {
      insert(record);
    }
  }

  @After
  public void dropTable() throws SQLException {

    try (Connection conn = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
      conn.setAutoCommit(true);
      Statement statement = conn.createStatement();
      statement.execute("DROP TABLE " + tableName);
    }
  }

  protected org.joda.time.Instant toSchemaDatetime(Instant updateTimestamp) {
    return org.joda.time.Instant.ofEpochMilli(updateTimestamp.toEpochMilli());
  }

  protected void updateRecord(int key, String newValue, Instant updateTime) throws SQLException {
    try (Connection conn = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
      conn.setAutoCommit(true);
      PreparedStatement statement = conn
          .prepareStatement("UPDATE " + tableName + " SET b = ?, ts = ? WHERE a = ?");
      statement.setString(1, newValue);
      statement.setTimestamp(2, Timestamp.from(updateTime),
          Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      statement.setInt(3, key);
      statement.execute();
    }
  }

  protected void insert(Data data) throws SQLException {
    try (Connection conn = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
      PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO table1 "
          + " VALUES (?, ?, ?)");
      preparedStatement.setInt(1, data.a);
      preparedStatement.setString(2, data.b);
      preparedStatement.setTimestamp(3, Timestamp.from(data.ts),
          Calendar.getInstance(TimeZone.getTimeZone("UTC")));
      preparedStatement.execute();
    }
  }
}
