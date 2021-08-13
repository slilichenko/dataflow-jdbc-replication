resource "google_project_service" "secret-manager-api" {
  service = "secretmanager.googleapis.com"
}
