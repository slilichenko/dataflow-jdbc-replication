resource "google_project_service" "secret-manager-api" {
  service = "secretmanager.googleapis.com"
}

resource "google_project_service" "dataflow-api" {
  service = "dataflow.googleapis.com"
}

resource "google_project_service" "cloud-build-api" {
  service = "cloudbuild.googleapis.com"
}
