FROM apache/flink:1.20.0-scala_2.12-java11

WORKDIR /opt/flink

# Define version variables
ENV HIVE_VERSION=3.1.3 \
    HADOOP_VERSION=3.3.4 \
    ICEBERG_VERSION=1.7.1 \
    AWS_SDK_VERSION=1.12.780 \
    AWS_BUNDLE_VERSION=2.30.14 \
    SHADED_GUAVA_VERSION=1.3.0 \
    FLINK_VERSION=1.20.0 \
    FLINK_VERSION_SHRT=1.20 \
    SCALA_VERSION=2.12 \
    CONFIGURATION2_VERSION=2.11.0 \
    LOGGING_VERSION=1.3.4 \
    STAX2_VERSION=4.2.2 \
    WOODSTOX_VERSION=7.1.0 \
    LANG3_VERSION=3.17.0

# Install required dependencies
RUN apt-get update && \
    apt-get install -y vim lnav net-tools

RUN echo "Add Flink S3 Plugin" && \
    mkdir ./plugins/s3-fs-hadoop && \
    cp ./opt/flink-s3-fs-hadoop-${FLINK_VERSION}.jar ./plugins/s3-fs-hadoop/

RUN echo "-> Install JARs: Flink's Hive connector" && \
    mkdir -p ./lib/hive && \
    curl https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-hive-${HIVE_VERSION}_${SCALA_VERSION}/${FLINK_VERSION}/flink-sql-connector-hive-${HIVE_VERSION}_${SCALA_VERSION}-${FLINK_VERSION}.jar -o ./lib/hive/flink-sql-connector-hive-${HIVE_VERSION}_2.12-${FLINK_VERSION}.jar

RUN echo "-> Install JARs: Dependencies for Iceberg" && \
    mkdir -p ./lib/iceberg && \
    curl https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-flink-runtime-${FLINK_VERSION_SHRT}/${ICEBERG_VERSION}/iceberg-flink-runtime-${FLINK_VERSION_SHRT}-${ICEBERG_VERSION}.jar -o ./lib/iceberg/iceberg-flink-runtime-${FLINK_VERSION_SHRT}-${ICEBERG_VERSION}.jar

RUN echo "-> Install JARs: AWS / Hadoop S3" && \
    mkdir -p ./lib/aws && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar -o ./lib/aws/hadoop-aws-${HADOOP_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar -o ./lib/aws/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/${AWS_BUNDLE_VERSION}/bundle-${AWS_BUNDLE_VERSION}.jar -o ./lib/aws/bundle-${AWS_BUNDLE_VERSION}.jar

RUN echo "-> Install JARs: Hadoop" && \
    mkdir -p ./lib/hadoop && \
    curl https://repo1.maven.org/maven2/org/apache/commons/commons-configuration2/${CONFIGURATION2_VERSION}/commons-configuration2-${CONFIGURATION2_VERSION}.jar -o ./lib/hadoop/commons-configuration2-${CONFIGURATION2_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/commons-logging/commons-logging/${LOGGING_VERSION}/commons-logging-${LOGGING_VERSION}.jar -o ./lib/hadoop/commons-logging-${LOGGING_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/${LANG3_VERSION}/commons-lang3-${LANG3_VERSION}.jar -o ./lib/hadoop/commons-lang3-${LANG3_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-auth/${HADOOP_VERSION}/hadoop-auth-${HADOOP_VERSION}.jar -o ./lib/hadoop/hadoop-auth-${HADOOP_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-common/${HADOOP_VERSION}/hadoop-common-${HADOOP_VERSION}.jar -o ./lib/hadoop/hadoop-common-${HADOOP_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/thirdparty/hadoop-shaded-guava/${SHADED_GUAVA_VERSION}/hadoop-shaded-guava-${SHADED_GUAVA_VERSION}.jar -o ./lib/hadoop/hadoop-shaded-guava-${SHADED_GUAVA_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/codehaus/woodstox/stax2-api/${STAX2_VERSION}/stax2-api-${STAX2_VERSION}.jar -o ./lib/hadoop/stax2-api-${STAX2_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/com/fasterxml/woodstox/woodstox-core/${WOODSTOX_VERSION}/woodstox-core-${WOODSTOX_VERSION}.jar -o ./lib/hadoop/woodstox-core-${WOODSTOX_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-hdfs-client/${HADOOP_VERSION}/hadoop-hdfs-client-${HADOOP_VERSION}.jar -o ./lib/hadoop/hadoop-hdfs-client-${HADOOP_VERSION}.jar && \
    curl https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-mapreduce-client-core/${HADOOP_VERSION}/hadoop-mapreduce-client-core-${HADOOP_VERSION}.jar -o ./lib/hadoop/hadoop-mapreduce-client-core-${HADOOP_VERSION}.jar

RUN echo "-> Install JARs: Http-Connector" && \
    mkdir -p ./lib/http-conn && \
    curl https://repo1.maven.org/maven2/com/getindata/flink-http-connector/0.19.0/flink-http-connector-0.19.0.jar -o ./lib/http-conn/flink-http-connector-0.19.0.jar \
    curl https://repo1.maven.org/maven2/org/apache/flink/flink-clients/${FLINK_VERSION}/flink-clients-${FLINK_VERSION}.jar -o ./lib/http-conn/flink-clients-${FLINK_VERSION}.jar \
    curl https://repo1.maven.org/maven2/org/apache/flink/flink-java/${FLINK_VERSION}/flink-java-${FLINK_VERSION}.jar -o ./lib/http-conn/flink-java-${FLINK_VERSION}.jar \
    curl https://repo1.maven.org/maven2/org/apache/flink/flink-connector-base/${FLINK_VERSION}/flink-connector-base-${FLINK_VERSION}.jar -o ./lib/http-conn/flink-connector-base-${FLINK_VERSION}.jar

RUN echo "Purge build artifacts" && \
    apt-get purge -y --auto-remove $build_deps && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Enable SQL Client to find the job manager when running it from this image
RUN chown -R flink:flink lib/*
RUN chown -R flink:flink conf/*

CMD ["./bin/start-cluster.sh", "start-foreground"]
