package com.dataplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonFlattener implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(JsonFlattener.class);
    private final ObjectMapper objectMapper;

    public JsonFlattener() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> flatten(String jsonString, Map<String, Object> flatteningConfig) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        List<Map<String, Object>> resultRows = new ArrayList<>();
        processNode(rootNode, flatteningConfig, new LinkedHashMap<>(), resultRows);
        return resultRows;
    }

    private void processNode(JsonNode currentNode, Map<String, Object> config, Map<String, Object> currentRow, List<Map<String, Object>> resultRows) throws Exception {
        if (config == null || config.isEmpty()) {
            // If no more config, add the current row to results
            resultRows.add(new LinkedHashMap<>(currentRow));
            return;
        }

        if (config.containsKey("f.map_merge_with_rows")) {
            Map<String, Object> mapConfig = (Map<String, Object>) config.get("f.map_merge_with_rows");
            processMapMergeWithRows(currentNode, mapConfig, currentRow, resultRows);
        } else if (config.containsKey("f.array_of_maps_to_rows")) {
            Map<String, Object> arrayConfig = (Map<String, Object>) config.get("f.array_of_maps_to_rows");
            processArrayOfMapsToRows(currentNode, arrayConfig, currentRow, resultRows);
        } else {
            // This is a schema definition for a map
            Map<String, Object> newRow = new LinkedHashMap<>(currentRow);
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                String fieldName = entry.getKey();
                String fieldType = (String) entry.getValue(); // Assuming direct type for now

                JsonNode fieldNode = currentNode.get(fieldName);
                if (fieldNode != null) {
                    newRow.put(fieldName, convertValue(fieldNode, fieldType));
                } else {
                    LOG.warn("Field '{}' not found in current JSON node.", fieldName);
                }
            }
            resultRows.add(newRow);
        }
    }

    private void processMapMergeWithRows(JsonNode currentNode, Map<String, Object> mapConfig, Map<String, Object> currentRow, List<Map<String, Object>> resultRows) throws Exception {
        String parentKeyFieldName = null;
        if (mapConfig.containsKey("f._parent_key")) {
            parentKeyFieldName = (String) mapConfig.get("f._parent_key");
        }

        Map<String, Object> nextConfig = new LinkedHashMap<>(mapConfig);
        nextConfig.remove("f._parent_key"); // Remove special key from config for next level

        Iterator<Map.Entry<String, JsonNode>> fields = currentNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode valueNode = entry.getValue();

            Map<String, Object> newRow = new LinkedHashMap<>(currentRow);
            if (parentKeyFieldName != null) {
                newRow.put(parentKeyFieldName, key);
            }

            // Find the specific config for this key
            if (nextConfig.containsKey(key)) {
                Map<String, Object> subConfig = (Map<String, Object>) nextConfig.get(key);
                processNode(valueNode, subConfig, newRow, resultRows);
            } else if (nextConfig.size() == 1 && nextConfig.values().iterator().next() instanceof Map) {
                // This handles the case where there's a single nested map config,
                // implying it applies to all children of the current node.
                // This is a simplification and might need refinement based on exact requirements.
                Map<String, Object> defaultSubConfig = (Map<String, Object>) nextConfig.values().iterator().next();
                processNode(valueNode, defaultSubConfig, newRow, resultRows);
            } else {
                LOG.warn("No specific configuration found for key '{}' in map_merge_with_rows. Skipping.", key);
            }
        }
    }

    private void processArrayOfMapsToRows(JsonNode currentNode, Map<String, Object> arrayConfig, Map<String, Object> currentRow, List<Map<String, Object>> resultRows) throws Exception {
        for (Map.Entry<String, Object> entry : arrayConfig.entrySet()) {
            String arrayFieldName = entry.getKey();
            Map<String, Object> itemSchema = (Map<String, Object>) entry.getValue();

            JsonNode arrayNode = currentNode.get(arrayFieldName);
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode itemNode : arrayNode) {
                    Map<String, Object> newRow = new LinkedHashMap<>(currentRow);
                    for (Map.Entry<String, Object> schemaEntry : itemSchema.entrySet()) {
                        String fieldName = schemaEntry.getKey();
                        String fieldType = (String) schemaEntry.getValue();

                        JsonNode fieldNode = itemNode.get(fieldName);
                        if (fieldNode != null) {
                            newRow.put(fieldName, convertValue(fieldNode, fieldType));
                        } else {
                            LOG.warn("Field '{}' not found in array item. Skipping.", fieldName);
                        }
                    }
                    resultRows.add(newRow);
                }
            } else {
                LOG.warn("Array field '{}' not found or is not an array. Skipping array processing.", arrayFieldName);
            }
        }
    }

    private Object convertValue(JsonNode node, String type) {
        if (node == null || node.isNull()) {
            return null;
        }
        switch (type.toLowerCase()) {
            case "string":
                return node.asText();
            case "integer":
                return node.asInt();
            case "long":
                return node.asLong();
            case "double":
                return node.asDouble();
            case "boolean":
                return node.asBoolean();
            case "array<string>":
                if (node.isArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonNode element : node) {
                        list.add(element.asText());
                    }
                    return list;
                }
                return null;
            case "array<integer>":
                if (node.isArray()) {
                    List<Integer> list = new ArrayList<>();
                    for (JsonNode element : node) {
                        list.add(element.asInt());
                    }
                    return list;
                }
                return null;
            case "array<long>":
                if (node.isArray()) {
                    List<Long> list = new ArrayList<>();
                    for (JsonNode element : node) {
                        list.add(element.asLong());
                    }
                    return list;
                }
                return null;
            case "array<double>":
                if (node.isArray()) {
                    List<Double> list = new ArrayList<>();
                    for (JsonNode element : node) {
                        list.add(element.asDouble());
                    }
                    return list;
                }
                return null;
            case "array<boolean>":
                if (node.isArray()) {
                    List<Boolean> list = new ArrayList<>();
                    for (JsonNode element : node) {
                        list.add(element.asBoolean());
                    }
                    return list;
                }
                return null;
            default:
                LOG.warn("Unsupported type: {}. Returning raw text.", type);
                return node.asText();
        }
    }
}
