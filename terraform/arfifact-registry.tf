resource "google_artifact_registry_repository" "dataflow-template-repo" {
  provider = google-beta
  depends_on = [google_project_service.artifact-registry-api]

  location      = var.region
  project       = var.project_id
  repository_id = "dataflow-template-repo"
  description   = "Repository for Dataflow templates"
  format        = "DOCKER"
}

output "repo-name" {
  value = google_artifact_registry_repository.dataflow-template-repo.name
}

output "repo-location" {
  value = google_artifact_registry_repository.dataflow-template-repo.location
}

output "repo-project-id" {
  value = google_artifact_registry_repository.dataflow-template-repo.project
}