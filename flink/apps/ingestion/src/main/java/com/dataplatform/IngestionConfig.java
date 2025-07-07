package com.dataplatform;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Nullable;

class InvalidConfigValueException extends Exception {
    public InvalidConfigValueException(String message) {
        super(message);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionConfig implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(IngestionConfig.class);

    // Source Configuration
    private SourceConfig source;
    private Map<String, String> schema;
    private Map<String, Object> flatteningConfig;
    private DestinationConfig destination;
    private MetadataConfig metadata;
    private ProcessingConfig processing;
    private ErrorHandlingConfig errorHandling;
    private ValidationConfig validation;

    // Getters and Setters
    public SourceConfig getSource() {
        return source;
    }

    public void setSource(SourceConfig source) {
        this.source = source;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, String> schema) {
        this.schema = schema;
    }

    public Map<String, Object> getFlatteningConfig() {
        return flatteningConfig;
    }

    public void setFlatteningConfig(Map<String, Object> flatteningConfig) {
        this.flatteningConfig = flatteningConfig;
    }

    public String getSchemaString() {

        if (schema == null || schema.isEmpty()) {
            return "item String";
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : schema.entrySet()) {

            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(" ")
                    .append(entry.getValue())
                    .append(",");

        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public String getColumnList() {
        if (schema == null || schema.isEmpty()) {
            return "item";
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : schema.entrySet()) {

            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(",");

        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public DestinationConfig getDestination() {
        return destination;
    }

    public void setDestination(DestinationConfig destination) {
        this.destination = destination;
    }

    public MetadataConfig getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataConfig metadata) {
        this.metadata = metadata;
    }

    public ProcessingConfig getProcessing() {
        return processing;
    }

    public void setProcessing(ProcessingConfig processing) {
        this.processing = processing;
    }

    public ErrorHandlingConfig getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(ErrorHandlingConfig errorHandling) {
        this.errorHandling = errorHandling;
    }

    public ValidationConfig getValidation() {
        return validation;
    }

    public void setValidation(ValidationConfig validation) {
        this.validation = validation;
    }

    // Nested Configuration Classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SourceConfig implements Serializable {
        private String type;
        private String format;
        private String path;
        private ConnectionConfig connection;
        @Nullable
        private IterationConfig iteration;
        @Nullable
        private Map<String, String> parserOptions;
        @Nullable
        private String packaging;
        @Nullable
        private String arrayField;
        @Nullable
        private String mapField;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public ConnectionConfig getConnection() {
            return connection;
        }

        public void setConnection(ConnectionConfig connection) {
            this.connection = connection;
        }

        public IterationConfig getIteration() {
            return iteration;
        }

        public void setIteration(IterationConfig iteration) {
            this.iteration = iteration;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, String> getParserOptions() {
            return parserOptions;
        }

        public void setParserOptions(Map<String, String> parserOptions) {
            this.parserOptions = parserOptions;
        }

        public String getPackaging() {
            return packaging;
        }

        public void setPackaging(String packaging) {
            this.packaging = packaging;
        }

        public String getArrayField() {
            return arrayField;
        }

        public void setArrayField(String arrayField) {
            this.arrayField = arrayField;
        }

        public String getMapField() {
            return mapField;
        }

        public void setMapField(String mapField) {
            this.mapField = mapField;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectionConfig implements Serializable {
        private String url;
        private Map<String, String> headers;
        private AuthConfig auth;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public AuthConfig getAuth() {
            return auth;
        }

        public void setAuth(AuthConfig auth) {
            this.auth = auth;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthConfig implements Serializable {
        private String type;
        private Map<String, String> credentials;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getCredentials() {
            return credentials;
        }

        public void setCredentials(Map<String, String> credentials) {
            this.credentials = credentials;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IterationConfig implements Serializable {
        @Nullable
        private String mechanism;
        @Nullable
        private Map<String, String> parameters;
        @Nullable
        private String query;

        public String getMechanism() {
            return mechanism;
        }

        public void setMechanism(String mechanism) {
            this.mechanism = mechanism;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DestinationConfig implements Serializable {
        private String layer;
        private String table;
        private String format;
        private List<PartitionConfig> partitioning;

        public String getLayer() {
            return layer;
        }

        public void setLayer(String layer) {
            this.layer = layer;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public List<PartitionConfig> getPartitioning() {
            return partitioning;
        }

        public void setPartitioning(List<PartitionConfig> partitioning) {
            this.partitioning = partitioning;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartitionConfig implements Serializable {
        private String field;
        private String type;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataConfig implements Serializable {
        private String lineageKey;
        private Map<String, String> enrichment;
        private List<String> tags;

        public String getLineageKey() {
            return lineageKey;
        }

        public void setLineageKey(String lineageKey) {
            this.lineageKey = lineageKey;
        }

        public Map<String, String> getEnrichment() {
            return enrichment;
        }

        public void setEnrichment(Map<String, String> enrichment) {
            this.enrichment = enrichment;
        }

        public String getEnrichmentFieldsAsJsonString() {
            ObjectMapper objectMapper = new ObjectMapper();

            if (enrichment == null || enrichment.size() == 0) {
                return "{}";
            }

            try {
                return objectMapper.writeValueAsString(enrichment);

            } catch (JsonProcessingException e) {
                LOG.warn("Failed to convert metadata enrichment map to json");
                return "{}";
            }
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessingConfig implements Serializable {
        private String mode;
        private String cadence;
        private int taskCpu;
        private String taskMemory;
        private int parallelism;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getCadence() {
            return cadence;
        }

        public void setCadence(String cadence) {
            this.cadence = cadence;
        }

        public int getTaskCpu() {
            return taskCpu;
        }

        public void setTaskCpu(int taskCpu) {
            this.taskCpu = taskCpu;
        }

        public String getTaskMemory() {
            return taskMemory;
        }

        public void setTaskMemory(String taskMemory) {
            this.taskMemory = taskMemory;
        }

        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorHandlingConfig implements Serializable {
        private Integer maxRetries;
        private Integer retryDelay;
        private Double errorTolerance;
        private ErrorDestinationConfig errorDestination;

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Integer getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Integer retryDelay) {
            this.retryDelay = retryDelay;
        }

        public Double getErrorTolerance() {
            return errorTolerance;
        }

        public void setErrorTolerance(Double errorTolerance) {
            this.errorTolerance = errorTolerance;
        }

        public ErrorDestinationConfig getErrorDestination() {
            return errorDestination;
        }

        public void setErrorDestination(ErrorDestinationConfig errorDestination) {
            this.errorDestination = errorDestination;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDestinationConfig implements Serializable {
        private String layer;
        private String table;

        public String getLayer() {
            return layer;
        }

        public void setLayer(String layer) {
            this.layer = layer;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationConfig implements Serializable {
        private List<ValidationRuleConfig> rules;

        public List<ValidationRuleConfig> getRules() {
            return rules;
        }

        public void setRules(List<ValidationRuleConfig> rules) {
            this.rules = rules;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationRuleConfig implements Serializable {
        private String field;
        private String type;
        private Map<String, Object> parameters;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert IngestionConfig to string", e);
            return "{}";
        }
    }
}
