package com.dataplatform.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metadata {
    private static final Logger LOG = LoggerFactory.getLogger(Metadata.class);
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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
}
