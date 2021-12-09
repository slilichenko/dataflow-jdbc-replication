resource "google_service_account" "dataflow-worker-sa" {
  account_id = "dataflow-worker-sa"
  display_name = "Service Account for Dataflow worker"
}

resource "google_project_iam_member" "dataflow-worker-dataflow-role" {
  project = var.project_id
  role = "roles/dataflow.worker"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

// Required to access container registry
resource "google_project_iam_member" "dataflow-worker-storage-object-viewer" {
  project = var.project_id
  role = "roles/storage.objectViewer"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

// Required to access container registry
resource "google_project_iam_member" "dataflow-worker-sql-client" {
  project = var.project_id
  role = "roles/cloudsql.client"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}


output "dataflow-worker-sa" {
  value = google_service_account.dataflow-worker-sa.email
}