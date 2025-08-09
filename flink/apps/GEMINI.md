## Objective

A tool that can ingest data from a variety of sources (s3, apis, databases, etc), supporting both stream and batch processing, and load the data into an iceberg table in the bronze schema.  

## Configuration

The tool uses flink, deployed on kubernetes, and will be provided a configuration file with all the necessary information to determine how and where the data should be ingested and loaded onto our datalake. 

Each execution will grab the corresponding secret, hosted in kubernetes secrets, and contains the following yaml structure:

- source: (information about the source)
  - type: (the type of source, e.g. api, database, s3 or other cloud storage, etc)
  - format: format of the data (json, csv, etc)
  - packaging: packaging of the data (gz, zip, tar, etc)
  - auth: authorization method + credentials
  - flatteningInstructions: if the dataset is unstructured and needs to be flattened into row-based structure
  - ${type}: any type specific information 
    - e.g. api needs a host and path, s3 needs a path, database needs a query
- destination: details about the write destination 
  - table: the name of the table
  - schema: optional, if you want to write to a non-bronze schema
  - catalog: optional, if you want to write to a non default catalog
- processing: details about how to process for flink & airflow:
  - mode: batch/stream, for flink
  - cadence: use the airflow dag schedule syntax
  - parallelism: for flink
  - taskCpu: for flink
  - taskMemory: for flink
  - maxActiveRuns: for airflow
  - taskConcurrency: for airflow
- metadata: any additional metadata
  - enrichment: hardcode any extra fields/values to dataset at runtime, e.g.
    - schema_version: "1.0"
    - contains_pii: false
  - lineageKey: link dataset across data platform layers
  - tags: list to assign tags to airflow dag
- schema: map of field names + data types of data we'll be inserting into datalake
- freshnessSLA: how frequently new data should be made available - not implemented yet

IngestionConfig.java is used to hold the values from these configuration files.

## Deployment 

Each bronze configuration file will create an airflow DAG.  Inside each dag will be a task to use a kubernetes deployment template to launch a flink job as a per-job mode: ingestion/deployments/template.yaml.  We will not use a session cluster.

## Design Decisions 

We should always use the flink SQL API layer whenever possible.  The data stream API should be used only when absolutely necessary and the source cannot be ingested with the flink SQL API.

The application versions have been carefully tested, and should not be changed:

    <flink.version>1.20.0</flink.version>
    <iceberg.version>1.7.1</iceberg.version>
    <hive.version>3.1.3</hive.version>
    <hadoop.version>3.3.4</hadoop.version>
    <netty.version>4.1.118.Final</netty.version>

We will upgrade flink later, once version 2 is more mature.

## Flink Sources

Flink provides great support for reading data from many sources already (s3, kafka, and certain databases), and less support for things like API.  It is this projects goal to provide great support for as many types of source data as possible, both structure and unstructured data.

The HttpSource was built to provide a way to ingest data from APIs:

ingestion/src/main/java/com/dataplatform/sources/http/HttpSource.java

However, it does not currently do a great job at flattening unstructured data, we will work on that later.

Additional custom sources may needed to be built.
