# A Cloud-Agnostic Data Platform

A cloud-agnostic open-source data platform built to support both batch and streaming workflows.  Focused on configuration-driven pipelines and modular design. Provides a modern, robust end-to-end foundation that can be extended as needed. 

---

## Architecture & Status

| Service              | Status            | Outstanding Work   
| :------------------- | :---------------: | :----------------- 
| [Minikube](#minikube)             | mvp               | Migrate to a multi-node kubernetes cluster
| [Minio](#minio)                | mvp               | Enable tls, object encryption, durable persistance, policy-base access control
| [Apache Iceberg](#postgres---for-hive-metastore-catalog-persistance)       | done              |                     
| [Secret management](https://github.com/kmacdonald76/de_secrets/tree/main)    | mvp               | Team-based access to secrets
| [Trino](#trino)                | mvp               | Set auth via secrets
| [Apache Flink](#flink-operator)         | mvp               | More sources, more formats & packaging, more stream config support
| [DBT](https://github.com/kmacdonald76/de_data_models)                  | mvp               | PII handling
| [CI/CD](#cicd)                | mvp               | Automate execution
| [Apache Superset](#superset)      | mvp               | Setup auth via secrets
| [Apache Airflow](#airflow)       | mvp               | External database
| [Centralized Logging](#centralized-logging)  | mvp               | Enable dashboard persistance
| Semantic Layer       | not-started       | Business friendly method of turning fact + dimensions into metrics + insights
| OpenMetadata         | not-started       | 
| Webhook + Export ETL | not-started       |



![Architecture Diagram](https://github.com/kmacdonald76/de_platform/blob/main/design/architecture.jpg?raw=true)

## Productionization

| Category             | Status            | Outstanding Work   
| :------------------ | :---------------: | :----------------- 
| Credentials   | not-started       | Set proper credentials for all necessary services
| Scope   | not-started       | Multi-node, Scalable, High-availability
| Networking   | not-started       | CNI, LoadBalancers, Proper cluster and node DNS
| Storage   | not-started       | Remove all hostPath references (mostly postgres), Docker Repository
| Ingress   | not-started       | Nginx (or alternative)
| Resource limits   | not-started       | Define and set to ensure fairness + stability
| Security   | not-started       | RBAC, NetworkPolicies, HTTPS
| External Access   | not-started       | Real IP, Domain name, Gateway, Homepage w/ Service Listings


---

## Pre-req:

<details>
    <summary>Requirements</summary>

- **curl**
- **psql**
- **mvn**
- **jq**
  - **Debian:**  
    ```bash
    sudo apt install curl postgresql-client maven jq
    ```
- **docker**  
  [Debian Instructions](https://docs.docker.com/engine/install/debian/#install-using-the-repository)
- **java 17+**
  - **Debian:**  
    ```bash
    curl -o jdk21.deb https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb
    sudo dpkg -i jdk21.deb
    ```
- **kubectl:**  
  [Official Instructions](https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/)
  - **Linux:**
    ```bash
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
    ```
- **Dedicated user for docker mgmt:**  
  [Official Instructions](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user)
  - **Linux:**
    ```bash
    sudo groupadd docker
    sudo usermod -aG docker $USER
    newgrp docker
    docker run hello-world
    ```
- **helm**  
  [Github Releases](https://github.com/helm/helm/releases)
- **mc** *(minio client)*
  - **Linux:**  
    ```bash
    curl https://dl.min.io/client/mc/release/linux-amd64/mc -o mc
    sudo install mc /usr/local/bin/mc && rm mc
    ```
- **trino-cli:**
  - **Linux:**
    ```bash
    curl -o trino https://repo1.maven.org/maven2/io/trino/trino-cli/468/trino-cli-468-executable.jar
    chmod +x trino
    sudo install -o root -g root -m 0755 trino /usr/local/bin/trino
    ```
</details>

---

## Minikube

**Install Minikube**  
  [Official Instructions](https://minikube.sigs.k8s.io/docs/start/?arch=%2Flinux%2Fx86-64%2Fstable%2Fbinary+download)
  ```bash
  curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
  sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
  ```

**Start cluster (set limits based on your hardware)**
  ```bash
  minikube start [--driver=kvm2] --cpus=12 --memory=32g --disk-size=100g --mount --mount-string="/data:/data"
  ```

<details>
  <summary><b>Optional Steps</b></summary>

**Install Kvm2 Driver**

By default, minikube will use your hosts docker for hosting minikube, which doesn't offer great resource isolation, but it will still work.

[Installation Instructions](https://minikube.sigs.k8s.io/docs/drivers/kvm2/)

**Enable Addons**
  ```bash
  minikube addons enable ingress
  minikube addons enable ingress-dns
  minikube addons enable metrics-server
  minikube addons enable storage-provisioner
  kubectl apply -f storage/local_storage.yaml
  ```

**Pod monitoring**

[k9s](https://github.com/derailed/k9s) is a nice tool to visualize pod statuses.

  ```bash
  k9s --readonly -A
  ```

</details>

---

## Secret Management

The secret manager sits at the core of this data platform.  It hosts the instructions for the creation of data pipelines, and content needed for deploying nearly all services.

Follow the instructions in this repository to set this up: [de_secrets](https://github.com/kmacdonald76/de_secrets/tree/main)

---

## DNS

Integrate your local machine's DNS with Minikube

[Official Instructions](https://minikube.sigs.k8s.io/docs/handbook/addons/ingress-dns/#Linux)

  # debian - NetworkManager
  ```bash
  sudo mkdir -p /etc/NetworkManager/dnsmasq.d/
  echo "server=/deplatform.local/$(minikube ip)" | sudo tee /etc/NetworkManager/dnsmasq.d/minikube.conf
  ```

---

## Minio

*First, set your minio credentials in the secret manager and sync to minikube*
  ```bash
  secrets/services/minio/raw_root_user.yaml
  secrets/services/minio/raw_root_password.yaml
  ```

**Deploy**

  ```bash
  kubectl create namespace minio
  kubectl config set-context --current --namespace=minio
  kubectl apply -f minio/pv.yaml -n minio
  kubectl apply -f minio/deployment.yaml -n minio
  kubectl apply -f minio/ingress.yaml -n minio
  ```

**Create minio buckets for storing warehouse data, flink apps, drivers, airflow logs**
  ```bash
  mc alias set local http://minio.deplatform.local:32000 <user> <password> # replace user/password with your credentials
  mc mb local/iceberg
  mc mb local/flink-apps
  mc mb local/drivers
  mc mb local/airflow
  ```

<details>
  <summary><b>Optional Steps</b></summary>

**Check status**
  ```bash
  minikube service minio-service -n minio
  kubectl get services -n minio
  kubectl get po -n minio
  ```

**Setup sample data**
  ```bash
  curl https://foodb.ca/public/system/downloads/foodb_2020_04_07_json.zip -o foodb_2020_04_07_json.zip
  mc cp foodb_2020_04_07_json.zip local/iceberg/landing/
  rm -rf foodb_2020_04_07_json.zip
  ```

[Web UI](http://minio.deplatform.local:32001/)

**Optional commands**
  ```bash
  ## delete minio
  kubectl config set-context --current --namespace=minio
  kubectl delete -f minio/ingress.yaml
  kubectl delete deployment minio-deployment -n minio
  kubectl delete service minio-service -n minio
  kubectl delete -f minio/pv.yaml -n minio
  ```
</details>

---

## Postgres - for hive metastore catalog persistance

*First, set your config & credentials in the secret manager and sync to minikube*
  ```bash
  secrets/services/iceberg/raw_postgres_conf.yaml
  secrets/services/iceberg/raw_postgres_hba_conf.yaml
  secrets/services/iceberg/raw_postgres_password.yaml
  secrets/services/iceberg/raw_postgres_user.yaml
  ```

**Deploy**

  ```bash
  kubectl create namespace iceberg
  kubectl config set-context --current --namespace=iceberg
  kubectl apply -f iceberg/postgres/pv.yaml -n iceberg
  kubectl apply -f iceberg/postgres/deployment.yaml -n iceberg
  kubectl apply -f iceberg/postgres/ingress.yaml -n iceberg
  ```

**Create database for hive metastore**
  ```bash
  psql -h postgres.deplatform.local --username postgres -p 32345 -c "CREATE DATABASE hive;"
  ```

<details>
  <summary>Optional Commands</summary>

  ```bash
  ## service details
  minikube service postgres -n iceberg
  ```

  ```bash
  ## connect to database
  psql -h postgres.deplatform.local --username postgres -p 32345 hive
  ```
  
  ```bash
  ## open shell on container
  kubectl exec -it postgres-0 -n iceberg -- /bin/bash
  ```
  
  ```bash
  ## delete postgres
  psql -h postgres.deplatform.local --username postgres -p 32345 -c "DROP DATABASE hive;"
  kubectl delete service postgres -n iceberg --ignore-not-found
  kubectl delete -f iceberg/postgres/ingress.yaml -n iceberg
  kubectl delete -f iceberg/postgres/deployment.yaml -n iceberg --ignore-not-found
  kubectl delete -f iceberg/postgres/pv.yaml -n iceberg --ignore-not-found
  ```
</details>

---

## Hive Metastore - Catalog for Iceberg table management

*First, set your config & credentials in the secret manager and sync to minikube*
  ```bash
  secrets/services/iceberg/raw_hive_conf.yaml
  ```

**Build custom docker image & load into minikube**
  ```bash
  docker build iceberg/hive/ -t hive-iceberg:dev
  minikube image load hive-iceberg:dev
  ```

**Deploy**
  ```bash
  kubectl config set-context --current --namespace=iceberg
  kubectl apply -f iceberg/hive/pv.yaml -n iceberg
  kubectl apply -f iceberg/hive/deployment.yaml -n iceberg
  kubectl apply -f iceberg/hive/ingress.yaml -n iceberg
  ```

**Verify connection**
  ```bash
  telnet metastore.deplatform.local 32083
  ```

<details>
  <summary>Optional Commands</summary>

  ```bash
  # open shell on container
  kubectl exec -it $(kubectl get pods -n iceberg | egrep -o "hive-metastore-\S+") -n iceberg -- bash
  ```
  ```bash
  # view schema initialization logs
  kubectl logs -f $(kubectl get pods -n iceberg | egrep -o "hive-metastore-\S+") -c hive-schema-init -n iceberg 
  ```
  ```bash
  # view main container logs
  kubectl logs -f $(kubectl get pods -n iceberg | egrep -o "hive-metastore-\S+") -c hive-metastore -n iceberg 
  ```
  ```bash
  # describing pod
  kubectl describe pod $(kubectl get pods -n iceberg | egrep -o "hive-metastore-\S+") -n iceberg
  ```
  ```bash
  # delete docker images
  minikube image rm hive-iceberg:dev
  docker image rm hive-iceberg:dev
  ```
  ```bash
  # delete hive metastore
  kubectl delete -f iceberg/hive/ingress.yaml -n iceberg
  kubectl delete -f iceberg/hive/deployment.yaml -n iceberg --ignore-not-found
  kubectl delete -f iceberg/hive/configMap.yaml -n iceberg --ignore-not-found
  kubectl delete -f iceberg/hive/pv.yaml -n iceberg --ignore-not-found
  ```
</details>

---

## Flink Operator

*First, set your config & credentials in the secret manager and sync to minikube*
  ```bash
  secrets/services/flink/hadoop_conf.yaml
  secrets/services/flink/iceberg_properties.yaml
  secrets/services/flink/raw_aws_access_key.yaml
  secrets/services/flink/raw_aws_secret_key.yaml
  secrets/services/flink/raw_flink_properties.yaml
  secrets/services/flink/raw_hive_conf.yaml
  secrets/services/flink/table_ingest.yaml
  ```

**Install cert manager**
  ```bash
  kubectl create -f https://github.com/jetstack/cert-manager/releases/download/v1.8.2/cert-manager.yaml
  ```
**Compile Flink Operator v1.12**

 *Latest official release (v1.11) contains [a bug](https://issues.apache.org/jira/browse/FLINK-37370) that doesn't allow us to track the lifecycle of application cluster*

  ```bash
  git clone https://github.com/apache/flink-kubernetes-operator.git ~/flink-kubernetes-operator
  mvn clean install -DskipTests -f ~/flink-kubernetes-operator/
  docker build ~/flink-kubernetes-operator/ -t flink-kubernetes-operator:1.12
  minikube image load flink-kubernetes-operator:1.12
  kubectl create namespace flink-ops
  ```

 *Clone the repo in a folder outside this repo to avoid cross compilation issues*

**Deploy**
  ```bash
  kubectl config set-context --current --namespace=flink-ops
  helm install -f flink/operator/postStart.yaml flink-kubernetes-operator ~/flink-kubernetes-operator/helm/flink-kubernetes-operator
  ```

<details>
  <summary>Optional Commands</summary>

  ```bash
  # open shell on flink operator
  kubectl exec -it $(kubectl get pods -n flink-ops | egrep -o "flink-kubernetes-op\S+") -n flink-ops -- /bin/bash
  ```

  ```bash
  # monitor log
  kubectl logs -f $(kubectl get pods -n flink-ops | egrep -o "flink-kubernetes-op\S+") -n flink-ops
  ```
  
  ```bash
  # delete flink operator
  kubectl config set-context --current --namespace=flink-ops
  helm uninstall flink-kubernetes-operator --ignore-not-found

  # 
  minikube image rm flink-kubernetes-operator:1.12
  docker image rm flink-kubernetes-operator:1.12
  ```
</details>

---

## Flink Session Cluster

**Build session cluster image & namespace**
  ```bash
  docker build flink/session/ -t flink-iceberg-hive:dev
  minikube image load flink-iceberg-hive:dev
  kubectl create namespace flink
  ```

**Deploy**
  ```bash
  kubectl config set-context --current --namespace=flink
  kubectl apply -f flink/role.yaml -n flink
  kubectl apply -f flink/session/deployment.yaml -n flink
  kubectl apply -f flink/session/ingress.yaml -n flink
  ```

<details>
  <summary>Optional Commands</summary>

  ```bash
  # view job manager logs
  kubectl logs -f $(kubectl get pods -n flink | egrep -o 'flink-session-\S+' | grep -v task) -n flink
  ```
  ```bash
  # open terminal in session cluster
  kubectl exec -it $(kubectl get pods -n flink | egrep -o "flink-sess\S+" | grep -v task) -n flink -- /bin/bash
  ```
  ```bash
  # open sql-client in session cluster
  kubectl exec -it $(kubectl get pods -n flink | egrep -o "flink-sess\S+" | grep -v task) -n flink -- ./bin/sql-client.sh
  ```
  ```bash
  # check job list on session cluster:
  curl http://flink.deplatform.local/jobs/overview
  ```
  ```bash
  # delete session cluster
  kubectl delete -f flink/session/ingress.yaml -n flink
  kubectl delete -f flink/session/deployment.yaml -n flink
  kubectl delete flinkdeployment flink-session -n flink --ignore-not-found
  ```
  ```bash
  # delete service account
  kubectl delete -f flink/role.yaml -n flink
  ```
  ```bash
  # delete minikube docker image
  minikube image rm flink-iceberg-hive:dev
  docker image rm flink-iceberg-hive:dev
  ```
</details>

---

## Setup Iceberg Databases

**Open sql-client in session cluster**
  ```bash
  kubectl exec -it $(kubectl get pods -n flink | egrep -o "flink-sess\S+") -n flink -- ./bin/sql-client.sh
  ```

**Run SQL through the sql-client terminal**

*(replace IP address w/ your minikube IP - get via command `minikube ip` and access key/secret)*

  ```sql
  CREATE CATALOG lakehouse WITH (
    'type'='iceberg',
    'catalog-type'='hive',
    'uri'='thrift://metastore.deplatform.local:32083',
    'warehouse'='s3a://iceberg/',
    's3.endpoint'='http://minio.deplatform.local:32000',
    's3.access-key'='data', 
    's3.secret-key'='engineer',
    's3.path.style.access'='true'
  );
  ```
  ```sql
  USE CATALOG lakehouse;
  ```
  ```sql
  CREATE DATABASE source;
  ```
  ```sql
  CREATE DATABASE bronze;
  ```
  ```sql
  CREATE DATABASE silver;
  ```
  ```sql
  CREATE DATABASE gold;
  ```


<details>
  <summary>Optional Steps</summary>

**Upload a sample newline json file to minio**

  ```bash
  mc cp ~/Downloads/test.json local/iceberg/landing/
  ```

**Optional, Load sample data into test table**

  ```sql
  CREATE TABLE `bronze`.`brz_foodb_ca__test` (
      line STRING
  ) WITH (
      'format-version'='2'
  );
  ```

  ```sql
  INSERT INTO `bronze`.`brz_foodb_ca__test`
  SELECT *
  FROM TABLE(
    SOURCE(
      'filesystem',
      's3a://iceberg/landing/test.json',
      'json'
    )
  );
  ```

</details>

**Recommended, Delete session cluster to free up memory**

  ```bash
  kubectl delete flinkdeployment flink-session -n flink --ignore-not-found
  ```

---

## Flink Apps

**Compile and upload jar**
  ```bash
  mvn clean package -f flink/apps/
  mc cp flink/apps/ingestion/target/ingestion-1.0.0.jar local/flink-apps/
  ```

<details>
  <summary>Optional Commands</summary>

**Deploy/Debug an App**
  ```bash
  kubectl config set-context --current --namespace=flink
  kubectl apply -f flink/apps/ingestion/deployments/<your_test_deployment>.yaml -n flink
  kubectl apply -f flink/role.yaml -n flink
  ```

  ```bash
  # tail job manager logs:
  kubectl logs -f $(kubectl get pods -n flink | egrep -o 'ingest-\S+' | grep -v task) -n flink
  ```
  ```bash
  # tail task manager logs:
  kubectl logs -f $(kubectl get pods -n flink | egrep -o 'ingest-\S+taskmanager\S+') -n flink
  ```
  ```bash
  # tail operator logs:
  kubectl logs -f $(kubectl get pods -n flink-ops | egrep -o 'flink-kubernetes-operator\S+') -n flink-ops
  ```
  ```bash
  # delete application deployment
  kubectl config set-context --current --namespace=flink
  kubectl delete FlinkDeployment $(kubectl get FlinkDeployment -A | egrep -o "ingest\S+") -n flink

  # if not dropping.. remove finalizer line:
  kubectl edit FlinkDeployment $(kubectl get FlinkDeployment -A | egrep -o "ingest\S+") -n flink
  ```
</details>

---

## Stream Source Testing - weather-forecast

**Prepare docker image for data generation**  
  ```bash
  docker build sample_stream/ -t sample-stream:dev
  minikube image load sample-stream:dev
  ```

**Deploy Data Generation Service**  
  ```bash
  kubectl apply -f sample_stream/weatherForecast.yaml -n default
  ```

<details>
  <summary>Optional Commands</summary>

  ```bash
  -- delete data generation job
  kubectl delete -f sample_stream/weatherForecast.yaml -n default
  ```
  ```bash
  -- delete docker image
  minikube image rm sample-stream:dev
  ```
</details>

---


## Trino 


**Optional, Configure Trino**  
[Trino Config Options](https://trinodb.github.io/charts/charts/trino/)
  ```bash
  vim trino/config.yaml
  ```
**Add Repo & Namespace**
  ```bash
  helm repo add trino https://trinodb.github.io/charts
  kubectl create namespace trino
  ```

**Deploy**
  ```bash
  kubectl config set-context --current --namespace=trino
  helm install -f trino/config.yaml trino-cluster trino/trino -n trino
  kubectl apply -f trino/ingress.yaml -n trino
  ```
**Trino cli access**
  ```bash
  trino --server http://trino.deplatform.local
  ```

**Sql Commands**

  ```sql
  SELECT * FROM lakehouse.bronze.brz_foodb_ca LIMIT 20;
  ```

[Trino UI](http://trino.deplatform.local)

<details>
  <summary>Optional Commands</summary>

  ```bash
  # attach to coordinator
  kubectl exec -it $(kubectl get pods | egrep -o '^trino-cluster-trino-coordinator\S+') -- /bin/bash
  ```
  ```bash
  # view coordinator logs
  kubectl logs $(kubectl get pods | egrep -o '^trino-cluster-trino-coordinator\S+')
  ```
  ```bash
  # view worker logs
  kubectl logs $(kubectl get pods | egrep -o '^trino-cluster-trino-worker\S+')
  ```
  ```bash
  # delete trino cluster
  kubectl config set-context --current --namespace=trino
  helm uninstall trino-cluster -n trino
  kubectl delete -f trino/ingress.yaml -n trino --ignore-not-found
  ```
  ```bash
  # open shell on trino coordinate container
  kubectl exec -it $(kubectl get pods | egrep -o '^trino-cluster-trino-coordinator\S+' -n trino) -n trino -- /bin/sh
  ```
</details>

## Superset 

[Official Instructions](https://superset.apache.org/docs/installation/kubernetes)

**Add helm repo, create namespace**,
  ```bash
  helm repo add superset https://apache.github.io/superset
  kubectl create namespace superset
  ```

**Build Image that contains drivers**,
  ```bash
  docker build superset -t superset:dev
  minikube image load superset:dev
  ```


**Deploy**
  ```bash
  kubectl config set-context --current --namespace=superset
  helm upgrade --install --values superset/values.yaml superset superset/superset
  kubectl apply -f superset/ingress.yaml -n superset
  ```

**Web Portal**

[Superset UI](http://superset.deplatform.local)

<details>
  <summary>Optional Commands</summary>

  ```bash
  -- tail main superset pod logs
  kubectl logs -f $(kubectl get pods -n superset -l app=superset -o jsonpath="{.items[0].metadata.name}") -n superset
  ```

  ```bash
  -- delete superset
  kubectl config set-context --current --namespace=superset
  helm uninstall superset --ignore-not-found
  kubectl delete job $(kubectl get jobs -n superset | egrep -o "superset\S+") -n superset
  kubectl delete -f superset/ingress.yaml -n superset --ignore-not-found
  ```
</details>


## Airflow

[Official Documentation](https://airflow.apache.org/docs/helm-chart/stable/index.html)

**Add helm repo & namespace**
  ```bash
  helm repo add apache-airflow https://airflow.apache.org
  kubectl create namespace airflow
  ```

**Build Extended Docker Image**
  ```bash
  docker build airflow -t airflow:dev
  minikube image load airflow:dev
  ```

**Deploy**  

[Parameter Reference](https://airflow.apache.org/docs/helm-chart/stable/parameters-ref.html)
  ```bash
  kubectl config set-context --current --namespace=airflow
  helm upgrade --install --values airflow/values.yaml airflow apache-airflow/airflow
  kubectl apply -f airflow/ingress.yaml -n airflow
  kubectl apply -f airflow/secret_access.yaml -n airflow
  kubectl apply -f airflow/flink_access.yaml -n airflow
  ```
[Airflow Web UI](http://airflow.deplatform.local/)

<details>
  <summary>Optional Commands</summary>

  ```bash
  # webserver logs
  kubectl logs -f $(kubectl get pods -n airflow | egrep -o "airflow-webserver\S+") -n airflow
  ```
  ```bash
  # webserver shell
  kubectl exec -it $(kubectl get pods -n airflow | egrep -o "airflow-webserver\S+") -n airflow -- /bin/bash
  ```
  ```bash
  # manually execute a git-sync operation
  kubectl exec -it airflow-triggerer-0 -n airflow -c git-sync -- /git-sync
  ```
  ```bash
  # manually reprocess dags
  kubectl exec -it $(kubectl get pods -n airflow | egrep -o "airflow-scheduler\S+") -n airflow -- airflow dags reserialize
  ```
  ```bash
  # worker logs
  kubectl logs -f $(kubectl get pods -n airflow | egrep -o "ingest-\S+") -n airflow
  ```
  ```bash
  # uninstall airflow
  kubectl config set-context --current --namespace=airflow
  helm delete airflow --namespace airflow
  kubectl delete job airflow-run-airflow-migrations --ignore-not-found
  kubectl delete -f airflow/ingress.yaml -n airflow

  # for now, volumes requires manually listing + removing .. database productionizing needs revisiting
  kubectl delete pvc -n airflow ...
  kubectl delete pv -n airflow ...
  ```
  ```bash
  # clean out images
  minikube image rm airflow:dev
  docker image rm airflow:dev
  ```
</details>

## Centralized Logging

- Grafana = UI
- Alloy = Log collection
- Loki = Log storage framework & query engine

**Setup Infrastructure Components**
```bash
  kubectl create namespace logging
  helm repo add grafana https://grafana.github.io/helm-charts
  mc mb local/logging-chunk
  mc mb local/logging-rule
  mc mb local/logging-admin
```

**Deploy Loki**
  ```bash
  kubectl config set-context --current --namespace=logging
  helm upgrade --install --values logging/loki_values.yaml loki grafana/loki \
        --namespace logging \
        --set promtail.enabled=false \
        --set fluent-bit.enabled=false \
        --set grafana.enabled=false
  ```
**Deploy Alloy**
  ```bash
  kubectl config set-context --current --namespace=logging
  kubectl apply -f logging/alloy_config.yaml -n logging
  helm upgrade --install --values logging/alloy_values.yaml --namespace logging alloy grafana/alloy
  ```

**Deploy Grafana**
  ```bash
  kubectl config set-context --current --namespace=logging
  helm upgrade --install --values logging/grafana_values.yaml --namespace logging grafana grafana/grafana
  kubectl apply -f logging/ingress.yaml -n logging
  ```
  ```bash
  # Obtain grafana password (username=admin)
  kubectl get secret --namespace logging grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
  ```
[Grafana Web UI](http://grafana.deplatform.local/)


<details>
  <summary>Optional Commands</summary>

  ```bash
  # Test loki (can take a few min for gateway+write pod to come online if you see 502 error)
  kubectl port-forward --namespace logging svc/loki-gateway 3100:80

  curl -H "Content-Type: application/json" -XPOST -s "http://127.0.0.1:3100/loki/api/v1/push"  \
      --data-raw "{\"streams\": [{\"stream\": {\"job\": \"test\"}, \"values\": [[\"$(date +%s)000000000\", \"fizzbuzz\"]]}]}"

  curl "http://127.0.0.1:3100/loki/api/v1/query_range" --data-urlencode 'query={job="test"}' | jq .data.result
  ```
  ```bash
  # Delete loki
  helm delete loki --namespace logging
  ```

  ```bash
  # monitor alloy
  kubectl logs -f $(kubectl get pods -n logging | egrep -o "alloy\S+") -n logging
  ```

  ```bash
  # delete alloy
  helm delete alloy --namespace logging
  ```

  ```bash
  # Delete grafana
  kubectl delete -f logging/ingress.yaml -n logging
  helm delete grafana --namespace logging
  ```
</details>

## Data Platform Webpage

The homepage is useful as a dashboard for all the services you now have access to.

  ```bash
  docker build homepage/ -t homepage:dev
  minikube image load homepage:dev
  ```

  ```bash
  kubectl apply -f homepage/deployment.yaml -n default
  ```

[Homepage](http://homepage.deplatform.local/)


## Onboarding a new dataset

1. First, you must specify your bronze layer source in the [secrets](https://github.com/kmacdonald76/de_secrets/tree/main) repo to inform the platform where to ingest data from.

2. Second, optional, you can generate a silver layer configuration to flatten data, convert data types, and perform data cleaning.

3. Once the configuration files are created, use the following CI script to sync them to Kubernetes and bake the new models into the dbt docker image, and which makes them available for airflow:

  ```bash
  ./ci_onboard_new_secret.sh
  ```

4. Open the [Airflow UI](http://airflow.deplatform.local/) and enable your new pipeline.

*Rerun the CI script after making any further modifications to your configuration files.*

