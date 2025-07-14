package com.dataplatform.sources.http;

import com.dataplatform.model.FlatteningInstructions;
import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpSourceConfig implements Serializable {

    private final String url;
    private final String iterationMechanism;
    private final Map<String, String> iterationParameters;
    private final String sourceFormat;
    private final Map<String, String> schema;
    private final FlatteningInstructions flatteningInstructions;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> parserOptions;

    @JsonCreator
    public HttpSourceConfig(
            @JsonProperty("url") String url,
            @JsonProperty("iterationMechanism") String iterationMechanism,
            @JsonProperty("iterationParameters") Map<String, String> iterationParameters,
            @JsonProperty("sourceFormat") String sourceFormat,
            @JsonProperty("schema") Map<String, String> schema,
            @JsonProperty("parserOptions") Map<String, String> parserOptions,
            @JsonProperty("flatteningInstructions") FlatteningInstructions flatteningInstructions) {

        this.url = url;
        this.iterationMechanism = iterationMechanism;
        this.iterationParameters = iterationParameters;
        this.sourceFormat = sourceFormat;
        this.schema = schema;
        this.parserOptions = parserOptions;
        this.flatteningInstructions = flatteningInstructions;
    }

    // Constructor that builds from JSON string
    public HttpSourceConfig(String json) throws JsonProcessingException {
        HttpSourceConfig config = objectMapper.readValue(json, HttpSourceConfig.class);
        this.url = config.url;
        this.iterationMechanism = config.iterationMechanism;
        this.iterationParameters = config.iterationParameters;
        this.sourceFormat = config.sourceFormat;
        this.schema = config.schema;
        this.parserOptions = config.parserOptions;
        this.flatteningInstructions = config.flatteningInstructions;
    }

    // Convert the entire config to JSON string
    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    @JsonProperty("flatteningInstructions")
    public FlatteningInstructions getFlatteningInstructions() {
        return flatteningInstructions;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("iterationMechanism")
    public String getIterationMechanism() {
        return iterationMechanism;
    }

    @JsonProperty("iterationParameters")
    public Map<String, String> getIterationParameters() {
        return iterationParameters;
    }

    @JsonProperty("sourceFormat")
    public String getSourceFormat() {
        return sourceFormat;
    }

    @JsonProperty("schema")
    public Map<String, String> getSchema() {
        return schema;
    }

    @JsonProperty("parserOptions")
    public Map<String, String> getParserOptions() {
        return parserOptions;
    }

    // helper methods to get values from parameter list
    public int endDateOffset() {
        if (iterationParameters == null) {
            return 0; // Default value if parameters map is null
        }
        String offsetStr = iterationParameters.get("endDateOffset");
        if (offsetStr == null) {
            return 0; // Default value if not specified
        }
        try {
            return Integer.parseInt(offsetStr);
        } catch (NumberFormatException e) {
            return 0; // Default value if parsing fails
        }
    }

    public String startDate() {
        if (iterationParameters == null) {
            return null;
        }
        return iterationParameters.get("startDate");
    }
}
