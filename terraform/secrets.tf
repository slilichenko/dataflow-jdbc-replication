resource "google_secret_manager_secret" "jdbc-url" {
  secret_id = "jdbc-url"

  replication {
    automatic = true
  }
  depends_on = [google_project_service.secret-manager-api]
}

resource "google_secret_manager_secret_version" "current-jdbc-url" {
  secret = google_secret_manager_secret.jdbc-url.id

  # Details: https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory/blob/main/docs/jdbc-postgres.md
  secret_data = format("jdbc:postgresql:///%s?cloudSqlInstance=%s&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=%s&password=%s",
  google_sql_database.data.name,
  google_sql_database_instance.main.connection_name,
  google_sql_user.data-reader.name,
  google_sql_user.data-reader.password )
}

resource "google_secret_manager_secret_iam_member" "member" {
  secret_id = google_secret_manager_secret.jdbc-url.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

output "jdbc-url-secret-id" {
  value = google_secret_manager_secret_version.current-jdbc-url.name
}