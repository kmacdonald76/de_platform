package com.dataplatform.sources.http.impl;

import com.dataplatform.model.FlatteningInstructions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonFlattenerParser implements HttpRecordParser {

    private final Map<String, String> schema;
    private final FlatteningInstructions instructions;
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;
    private final String[] orderedFieldNames; // Store the ordered field names from the schema
    private final Map<String, Integer> fieldNameToIndexMap; // Map field name to its index in the Row

    public JsonFlattenerParser(Map<String, String> schema, FlatteningInstructions instructions) {
        this.schema = schema;
        this.instructions = instructions;
        this.objectMapper = new ObjectMapper();
        this.jsonPathConfig = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST)
                .build();

        // Initialize orderedFieldNames and fieldNameToIndexMap based on the provided
        // schema
        this.orderedFieldNames = schema.keySet().toArray(new String[0]);
        this.fieldNameToIndexMap = new HashMap<>();
        for (int i = 0; i < orderedFieldNames.length; i++) {
            fieldNameToIndexMap.put(orderedFieldNames[i], i);
        }
    }

    @Override
    public List<Row> parse(byte[] record) throws Exception {
        JsonNode rootNode = objectMapper.readTree(record);
        List<Row> resultRows = new ArrayList<>();

        String targetPath = instructions.getTarget();

        Object targetNodes = JsonPath.using(jsonPathConfig).parse(rootNode).read(targetPath);

        List<JsonNode> targetNodesToProcess = new ArrayList<>();

        if (targetNodes instanceof List) {
            List<JsonNode> nodes = (List<JsonNode>) targetNodes;
            if (nodes.size() == 1 && nodes.get(0).isArray()) {
                ArrayNode arrayNode = (ArrayNode) nodes.get(0);
                arrayNode.forEach(targetNodesToProcess::add);
            } else {
                targetNodesToProcess.addAll(nodes);
            }
        } else if (targetNodes instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) targetNodes;
            if (arrayNode.size() > 0 && arrayNode.get(0).isArray()) {
                arrayNode = (ArrayNode) arrayNode.get(0);
            }
            for (JsonNode node : arrayNode) {
                targetNodesToProcess.add(node);
            }
        }

        if (targetNodesToProcess.isEmpty()) {
            // If target is not found or empty, and parent fields are included, return a row
            // with only parent fields
            if (instructions.getIncludeParentFields() != null
                    && !instructions.getIncludeParentFields().equals("none")) {
                Row emptyRow = new Row(schema.size());
                populateFields(rootNode, null, emptyRow); // Pass null for currentNode as there's no target data
                resultRows.add(emptyRow);
            }
            return resultRows;
        }

        int rowNum = 0;
        for (JsonNode targetNode : targetNodesToProcess) {
            Row row = new Row(schema.size());
            populateFields(rootNode, targetNode, row);
            resultRows.add(row);
        }

        return resultRows;
    }

    private void populateFields(JsonNode rootNode, JsonNode currentNode, Row row) {

        for (String fieldName : orderedFieldNames) {
            Object value = null;
            boolean fieldPopulated = false;

            // 1. Try to populate from target node (currentNode)
            if (currentNode != null) {
                // A. Check selectFields first, as it's the most specific instruction
                if (instructions.getSelectFields() != null && !instructions.getSelectFields().isEmpty()) {
                    for (FlatteningInstructions.SelectField selectField : instructions.getSelectFields()) {
                        if (selectField.getName().equals(fieldName)) {
                            JsonNode node = JsonPath.using(jsonPathConfig).parse(currentNode)
                                    .read(selectField.getPath());
                            if (node != null) {
                                // Pass handling instructions from the selectField if they exist
                                value = handleField(node.get(0), selectField.getArrayHandling(),
                                        selectField.getObjectHandling(), schema.get(fieldName));
                                fieldPopulated = true;
                            }
                            break; // Found the right selectField, no need to check others for this fieldName
                        }
                    }
                }

                // B. If not populated by selectFields, try direct mapping from currentNode
                if (!fieldPopulated && currentNode.has(fieldName)) {
                    JsonNode node = currentNode.get(fieldName);
                    value = handleField(node, instructions.getArrayHandling(), instructions.getObjectHandling(),
                            schema.get(fieldName));
                    fieldPopulated = true;
                }
            }

            // 2. If not populated from target, try to populate from parent node (rootNode)
            if (!fieldPopulated) {
                Object includeParentFields = instructions.getIncludeParentFields();
                if (includeParentFields != null) {
                    if (includeParentFields.equals("all")) {
                        if (rootNode.has(fieldName)) {
                            JsonNode node = rootNode.get(fieldName);
                            if (node != null && !node.isNull()) {
                                value = convertJsonNode(node, schema.get(fieldName));
                                fieldPopulated = true;
                            }
                        }
                    } else if (includeParentFields instanceof List) {
                        List<String> parentPaths = (List<String>) includeParentFields;
                        for (String path : parentPaths) {
                            // A simple check if the end of the path matches the field name
                            if (path.endsWith("." + fieldName) || path.equals(fieldName)) {
                                JsonNode node = JsonPath.using(jsonPathConfig).parse(rootNode).read(path);
                                if (node != null) {
                                    value = convertJsonNode(node, schema.get(fieldName));
                                    fieldPopulated = true;
                                    break; // Found a matching path
                                }
                            }
                        }
                    }
                }
            }

            if (fieldNameToIndexMap.containsKey(fieldName)) {
                row.setField(fieldNameToIndexMap.get(fieldName), value);
            }
        }
    }

    private Object handleField(JsonNode node, String arrayHandling, String objectHandling, String targetDataType) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            String effectiveArrayHandling = arrayHandling != null ? arrayHandling
                    : (instructions.getArrayHandling() != null ? instructions.getArrayHandling() : "json");
            switch (effectiveArrayHandling) {
                case "ignore":
                    return null;
                case "json":
                default:
                    return node.toString();
            }
        }

        if (node.isObject()) {
            String effectiveObjectHandling = objectHandling != null ? objectHandling
                    : (instructions.getObjectHandling() != null ? instructions.getObjectHandling() : "json");
            switch (effectiveObjectHandling) {
                case "ignore":
                    return null;
                case "json":
                default:
                    return node.toString();
            }
        }

        // Convert primitive types
        return convertJsonNode(node, targetDataType);
    }

    private Object convertJsonNode(JsonNode node, String dataType) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            if (node.size() == 1) {
                node = node.get(0);
            } else {
                return node.toString();
            }
        }

        switch (dataType.toLowerCase()) {
            case "string":
                return node.asText();
            case "integer":
                if (node.isNumber()) {
                    return node.asInt();
                } else {
                    try {
                        return Integer.parseInt(node.asText().replaceAll(",", ""));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            case "long":
                if (node.isNumber()) {
                    return node.asLong();
                } else {
                    try {
                        return Long.parseLong(node.asText().replaceAll(",", ""));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            case "double":
                if (node.isNumber()) {
                    return node.asDouble();
                } else {
                    try {
                        return Double.parseDouble(node.asText().replaceAll(",", ""));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            case "boolean":
                if (node.isBoolean()) {
                    return node.asBoolean();
                } else {
                    return Boolean.parseBoolean(node.asText());
                }
            default:
                return node.asText();
        }
    }

    public RowTypeInfo getRowTypeInfo() {
        String[] fieldNames = schema.keySet().toArray(new String[0]);

        TypeInformation<?>[] fieldTypes = schema.values().stream()
                .map(dataType -> {
                    switch (dataType.toLowerCase()) {
                        case "string":
                            return Types.STRING;
                        case "integer":
                            return Types.INT;
                        case "long":
                            return Types.LONG;
                        case "double":
                            return Types.DOUBLE;
                        case "boolean":
                            return Types.BOOLEAN;
                        default:
                            return Types.STRING; // Default to string
                    }
                })
                .toArray(TypeInformation<?>[]::new);

        return new RowTypeInfo(fieldTypes, fieldNames);
    }
}
