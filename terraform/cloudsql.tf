resource "google_sql_database_instance" "main" {
  name             = "main-instance"
  database_version = "POSTGRES_11"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}

resource "google_sql_database" "data" {
  name     = "data"
  instance = google_sql_database_instance.main.name
}

resource "google_sql_user" "data-reader" {
  name     = "data-reader"
  instance = google_sql_database_instance.main.name
  password = var.data_reader_password
}

resource "google_sql_user" "postgres" {
  name     = "postgres"
  instance = google_sql_database_instance.main.name
  password = var.postgres_password
}

output "postgresql_instance" {
  value = google_sql_database_instance.main.name
}

output "postgresql_database" {
  value = google_sql_database.data.name
}

output "postgresql_connection_name" {
  value = google_sql_database_instance.main.connection_name
}