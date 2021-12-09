variable "project_id" {
  type = string
}
variable "region" {
  type    = string
  default = "us-central1"
}

output "region" {
  value = var.region
}

variable "data_reader_password" {
  type      = string
  sensitive = true
}

variable "postgres_password" {
  type      = string
  sensitive = true
}