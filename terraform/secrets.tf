resource "google_secret_manager_secret" "sap-db-url" {
  secret_id = "sap-db-url"

  replication {
    automatic = true
  }
  depends_on = [google_project_service.secret-manager-api]
}

resource "google_secret_manager_secret_version" "version" {
  secret = google_secret_manager_secret.sap-db-url.id

  secret_data = var.db_url
}

resource "google_secret_manager_secret_iam_member" "member" {
  secret_id = google_secret_manager_secret.sap-db-url.secret_id
  role = "roles/secretmanager.secretAccessor"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

output "database-url-secret-id" {
  value = google_secret_manager_secret_version.version.name
}