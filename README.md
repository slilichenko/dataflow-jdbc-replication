# Dataflow JDBC Synchronization

## Overview
This project has a Beam pipeline which will copy data extracted from a JDBC 
data source into a set of BigQuery tables. It uses SAP as the source database, 
but can support any data source which has a JDBC driver.

## Setup
The setup process uses Terraform to create the environment. 
It uses [Secret Manager](https://cloud.google.com/secret-manager/docs) 
to secure JDBC username and password.

* Create a file with the password for the account which will be used by the piple to read the database, `data_reader.pass`
* Create a file with the password for the `postgres` admin user, `postres.pass`

In production environments use randomly generated passwords. One option is to use the `openssl` tool:
```bash
openssl rand -hex 20 > data_reader.pass &&  chmod g-r,o-r data_reader.pass
openssl rand -hex 20 > postgres.pass &&  chmod g-r,o-r postgres.pass
```
To run this tutorial you can put manually edit the password files; just make sure they don't have any extra lines and the passwords don't contain any spaces.
* Set up environment variables, create the environment and stage the pipeline:
  * `export PROJECT_ID=<Id of the Google Cloud Platform's project to deploy all artifacts>`
  * `export TF_VAR_data_reader_password=$(cat data_reader.pass)`
  * `export TF_VAR_postgres_password=$(cat postgres.pass)`
  * `source ./setup-env.sh`
  * `cd pipeline`
  * `./stage-dataflow-template.sh`
 
## Create Job Extract Metadata File(s)
After `setup-env.sh` script is run it creates several GCS buckets. One of the buckets 
(its name is stored in `METADATA_BUCKET` environment variable) needs to contain the metadata files
used for each jobs (most of the time - table extraction). 

For each job you plan to run create <job_name>.json file in that bucket. An example of the file:
```json
{
  "lastSync": 1508000099.519089000,
  "query": "Select col1, col2, col3 from  \"DBNAME\".\"TABLE\" where updated_ts between ? and ?"
}
```
Attributes:
* lastSync - the last time the table was synced. This attrubite will be automatically updated by 
the pipeline after each successful data extraction.
* query - a valid query with two positional parameters. The first parameter is the start timestamp and 
the second is the end timestamp

# Run the pipeline
Make sure that you ran `source ./setup-env.sh` - it will set up several environment variables required by the run script.

For each run you also need to set up the following variables:
* JOB_LIST - comma separated list of jobs to process. 
* If your pipeline requires running in a specific network/subnetwork set these variables:
    * DF_WORKER_NETWORK - name of the network
    * DF_WORKER_SUBNETWORK - name of the subnetwork in `https://www.googleapis.com/compute/v1/projects/{project_id}/regions/{region}/subnetworks/{subnetwork}` format

`./run-on-dataflow.sh` will start the pipeline and print the job details. 
You can monitor the progress of the pipeline in the GCP console.


 
