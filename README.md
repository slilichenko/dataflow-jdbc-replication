# Dataflow JDBC Synchronization

## Overview
This project contains a Beam pipeline which will copy data extracted from a JDBC 
data source into a set of Google Cloud Storage files in Avro format. It is designed
to be run on a regular basis and it maintains the state of the data extraction (last sync point) in
Google Cloud Storage files. Multiple extractions can be done in a single pipeline run.

This repo uses a Google Cloud SQL PostgeSQL instance to query the data, 
but any data source which has a JDBC driver can be used. JDBC connection URL is
stored in [Secret Manager](https://cloud.google.com/secret-manager/docs) to secure
database credentials.

## Setup
The setup process uses Terraform to create the environment.

* Create a file with the password for the account which will be used by the pipeline to read the database, `data_reader.pass`
* Create a file with the password for the `postgres` admin user, `postres.pass`

In production environments use randomly generated passwords. One option is to use the `openssl` tool:
```bash
openssl rand -hex 20 > data_reader.pass &&  chmod g-r,o-r data_reader.pass
openssl rand -hex 20 > postgres.pass &&  chmod g-r,o-r postgres.pass
```
You can put manually edit the password files; just make sure they don't have any extra lines and the passwords don't contain any spaces.
* Set up environment variables:
  * `export PROJECT_ID=<Id of the Google Cloud Platform's project to deploy all artifacts>`
  * `export TF_VAR_data_reader_password=$(cat data_reader.pass)`
  * `export TF_VAR_postgres_password=$(cat postgres.pass)`
* Create the environment. The script below runs the Terraform scripts, 
 a Flyway-based process to create the database schema, and exports several 
 environment variables:
  * `. ./setup-env.sh`
* Build and deploy the pipeline's Flex template
  * `cd pipeline`
  * `./stage-dataflow-template.sh`
 
## Creating Job Extract Metadata File(s)
After `setup-env.sh` script is run it creates several GCS buckets. One of the buckets 
(its name is stored in `METADATA_BUCKET` environment variable) needs to contain the metadata files
used for each jobs (most of the time - table extraction queries). These files are automatically  
updated by the pipeline to store the latest sync point.

The setup script automatically copies all the files located in [jobs](jobs) directory in that bucket. 
Notice that every time you run the this script or run `terraform apply` command the scripts in the bucket
will be overwritten by the scripts in the jobs folder.

If you need to add another job file - create a JSON file with .json extension 
and the following attributes:
* lastSync - the last time the table was synced. This attrubite will be automatically updated by 
the pipeline after each successful data extraction.
* query - a valid query with two positional parameters. The first parameter is the start timestamp and 
the second is the end timestamp

Example file:
```json
{
  "lastSync": 0.0,
  "query": "select * from customers where updated_ts between ? and ?"
}
```

# Run the process to continuously populate the database
A small Java app is used to simulate the continous data updates in the Cloud SQL database.
To run it:
* `cd client-app`
* `mvn package`
* `java -jar target/client-app-0.0.1-SNAPSHOT.jar`

# Run the pipeline
Make sure that you ran `source ./setup-env.sh` - it will set up several environment variables required by the run script.

For each run you also need to set up additional environment variables:
* JOB_LIST - comma separated list of jobs to process, e.g., `JOB_LIST=customers-sync,orders-sync,order-items-sync`
* If your pipeline requires running in a specific network/subnetwork set these variables:
    * DF_WORKER_NETWORK - name of the network
    * DF_WORKER_SUBNETWORK - name of the subnetwork in `https://www.googleapis.com/compute/v1/projects/{project_id}/regions/{region}/subnetworks/{subnetwork}` format

`./run-latest-template.sh` will find the last deployed template and run it.
`./run-on-dataflow.sh <template-spec-file>` can be used to run a particular version of the template.




 
