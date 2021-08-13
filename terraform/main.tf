provider "google" {
  project = var.project_id
  region  = var.region
}

data "google_project" "main" {
  project_id = var.project_id
}