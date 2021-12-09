resource "google_storage_bucket" "job_metadata" {
  project = var.project_id
  name = "${var.project_id}-sync-job-metadata"
  location = var.region

  uniform_bucket_level_access = true
}

resource "google_storage_bucket_object" "sync-job-definitions" {
  for_each = fileset("../jobs", "*.json")
  bucket = google_storage_bucket.job_metadata.name
  source = "../jobs/${each.value}"
  name = each.value
}

resource "google_storage_bucket_iam_member" "dataflow-worker-job-metadata-admin-role" {
  bucket = google_storage_bucket.job_metadata.name
  role    = "roles/storage.admin"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

resource "google_storage_bucket" "extracts" {
  project = var.project_id
  name = "${var.project_id}-sync-job-extract"
  location = var.region

  uniform_bucket_level_access = true
}
resource "google_storage_bucket_iam_member" "dataflow-worker-extracts-admin-role" {
  bucket = google_storage_bucket.extracts.name
  role    = "roles/storage.admin"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

resource "google_storage_bucket" "dataflow-temp" {
  project = var.project_id
  name = "${var.project_id}-dataflow-temp"
  location = var.region

  uniform_bucket_level_access = true
}
resource "google_storage_bucket_iam_member" "dataflow-worker-temp-viewer-role" {
  bucket = google_storage_bucket.dataflow-temp.name
  role    = "roles/storage.admin"
  member = "serviceAccount:${google_service_account.dataflow-worker-sa.email}"
}

output "metadata-bucket" {
  value = google_storage_bucket.job_metadata.name
}
output "output-bucket" {
  value = google_storage_bucket.extracts.name
}

output "df-temp-bucket" {
  value = google_storage_bucket.dataflow-temp.name
}