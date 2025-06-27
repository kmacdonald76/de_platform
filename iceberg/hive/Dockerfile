# Use Apache Hive 3.1.3 base image (or build from Hadoop)
FROM apache/hive:3.1.3

# Set working directory
WORKDIR /opt/hive

# Define version variables
ENV HIVE_VERSION=3.1.3 \
    HADOOP_VERSION=3.3.4 \
    ICEBERG_VERSION=1.7.1 \
    AWS_SDK_VERSION=1.12.780 \
    AWS_BUNDLE_VERSION=2.30.14 \
    SHADED_GUAVA_VERSION=1.3.0 \
    POSTGRES_VERSION=42.7.5

USER root
# Download required dependencies
RUN apt-get update && apt-get install -y wget curl unzip 
RUN rm -rf /var/lib/apt/lists/*

# Download Iceberg JARs

RUN echo "-> Install JARs: Iceberg" \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-hive-runtime/${ICEBERG_VERSION}/iceberg-hive-runtime-${ICEBERG_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-api/${ICEBERG_VERSION}/iceberg-api-${ICEBERG_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-common/${ICEBERG_VERSION}/iceberg-common-${ICEBERG_VERSION}.jar

# Download Hadoop AWS + S3A JARs
RUN echo "-> Install JARs: Hadoop AWS + S3A" \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-auth/${HADOOP_VERSION}/hadoop-auth-${HADOOP_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-common/${HADOOP_VERSION}/hadoop-common-${HADOOP_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/${AWS_BUNDLE_VERSION}/bundle-${AWS_BUNDLE_VERSION}.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar

# Download Guava (ensure compatibility)
RUN echo "-> Install JARs: Guava" \
    && rm -f lib/guava-*.jar \
    && wget -P lib/ https://repo1.maven.org/maven2/org/apache/hadoop/thirdparty/hadoop-shaded-guava/${SHADED_GUAVA_VERSION}/hadoop-shaded-guava-${SHADED_GUAVA_VERSION}.jar

# Download Postgres JDBC JAR
RUN echo "-> Install JARs: Postgres JDBC" \
    && wget -P lib/ https://jdbc.postgresql.org/download/postgresql-${POSTGRES_VERSION}.jar

USER hive

# Expose Hive Metastore Thrift Port
EXPOSE 9083

# Set environment variables
ENV SERVICE_NAME=metastore
ENV HIVE_HOME=/opt/hive
ENV HADOOP_HOME=/opt/hadoop
ENV PATH=$HIVE_HOME/bin:$PATH

# Run Hive Metastore on startup
CMD ["hive", "--service", "metastore", "-p", "9083"]
