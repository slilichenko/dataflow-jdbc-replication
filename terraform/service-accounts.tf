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

// Required to launch the template with the Docker image in Artifact Registry
resource "google_artifact_registry_repository_iam_member" "dataflow-worker-repo-writer" {
  provider = google-beta
  project = google_artifact_registry_repository.dataflow-template-repo.project
  location = google_artifact_registry_repository.dataflow-template-repo.location
  repository = google_artifact_registry_repository.dataflow-template-repo.name
  role = "roles/artifactregistry.reader"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

// Required to access SQL instance
resource "google_project_iam_member" "dataflow-worker-sql-client" {
  project = var.project_id
  role = "roles/cloudsql.client"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

output "dataflow-worker-sa" {
  value = google_service_account.dataflow-worker-sa.email
}

resource "google_service_account" "template-builder-sa" {
  account_id = "template-builder-sa"
  display_name = "Service Account for building Dataflow templates"
}

resource "google_artifact_registry_repository_iam_member" "template-builder-repo-writer" {
  provider = google-beta
  project = google_artifact_registry_repository.dataflow-template-repo.project
  location = google_artifact_registry_repository.dataflow-template-repo.location
  repository = google_artifact_registry_repository.dataflow-template-repo.name
  role = "roles/artifactregistry.writer"
  member = "serviceAccount:${google_service_account.template-builder-sa.email}"
}