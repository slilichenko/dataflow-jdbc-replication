# Dataflow JDBC Synchronization

## Overview
This project has a Beam pipeline which will copy data extracted from a JDBC data source.

## Setup
The setup process uses Terraform to create the environment.

* Set up required environment variables:
    * PROJECT_ID which contains Google Cloud project id
    * TF_VAR_db_url in 
     `jdbc:sap://<host>:<port>/?databaseName=<DBNAME>&user=YYY&password=ZZZ` format
* `cd pipeline`
* `source ./setup-env.sh`
* `./stage-dataflow-template.sh`
 
## Create Table Extract Metadata File(s)
After `setup-env.sh` script is run it creates several GCS buckets. One of the buckets 
(its name is stored in `METADATA_BUCKET` environment variable) needs to contain the metadata files
used for each table extraction. 

For each table you plan to extract create <table_name>.json file in that bucket. An example of the file:
```json
{
  "tableName": "table1",
  "lastSync": 1508000099.519089000,
  "query": "Select col1, col2, col3 from  \"DBNAME\".\"TABLE\" where updated_ts between ? and ?"
}
```
Attributes:
* tableName - must match the name of the table. TODO: it will be removed in the subsequent versions.
* lastSync - the last time the table was synced. This attrubite will be automatically updated by 
the pipeline after each successful data extraction.
* query - a valid query with two positional parameters. The first parameter is the start timestamp and 
the second is the end timestamp

# Run the pipeline
Make sure that you ran `source ./setup-env.sh` - it will set up several environment variables required by the run script.

For each run you also need to set up the following variables:
* TABLE_LIST - comma separated list of tables to process. 
* If your pipeline requires running in a specific network/subnetwork set these variables:
    * DF_WORKER_NETWORK - name of the network
    * DF_WORKER_SUBNETWORK - name of the subnetwork in `https://www.googleapis.com/compute/v1/projects/{project_id}/regions/{region}/subnetworks/{subnetwork}` format

`./run-on-dataflow.sh` will start the pipeline and print the job details. 
You can monitor the progress of the pipeline in the GCP console.


 
