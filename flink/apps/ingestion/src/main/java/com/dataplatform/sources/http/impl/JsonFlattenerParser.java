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

        // Initialize orderedFieldNames and fieldNameToIndexMap based on the provided schema
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

        Object rawTarget = JsonPath.using(jsonPathConfig).parse(rootNode)
                .read(instructions.getTarget());

        List<JsonNode> targetNodesToProcess = new ArrayList<>();

        if (rawTarget instanceof List) {
            for (Object item : (List<?>) rawTarget) {
                if (item instanceof JsonNode) {
                    targetNodesToProcess.add((JsonNode) item);
                }
            }
        } else if (rawTarget instanceof JsonNode) {
            JsonNode singleNode = (JsonNode) rawTarget;
            if (singleNode.isArray()) {
                for (JsonNode element : singleNode) {
                    targetNodesToProcess.add(element);
                }
            } else {
                targetNodesToProcess.add(singleNode);
            }
        }

        if (targetNodesToProcess.isEmpty()) {
            // If target is not found or empty, and parent fields are included, return a row with only parent fields
            if (instructions.getIncludeParentFields() != null
                    && !instructions.getIncludeParentFields().equals("none")) {
                Row emptyRow = new Row(schema.size());
                populateFields(rootNode, null, emptyRow); // Pass null for currentNode as there's no target data
                resultRows.add(emptyRow);
            }
            return resultRows;
        }

        for (JsonNode targetNode : targetNodesToProcess) {
            // If targetNode is an object and has pivot instructions
            if (targetNode.isObject() && instructions.getPivot() != null) {
                FlatteningInstructions.Pivot pivot = instructions.getPivot();
                Iterator<Map.Entry<String, JsonNode>> fields = targetNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode pivotedValue = JsonPath.using(jsonPathConfig).parse(entry.getValue())
                            .read(pivot.getValuePath());
                    Row row = new Row(schema.size());
                    // Populate fields, including the pivoted key as a column
                    populateFields(rootNode, pivotedValue, row, entry.getKey());
                    resultRows.add(row);
                }
            } else if (targetNode.isObject() && instructions.getKeyAsColumn() != null) {
                // If targetNode is an object and has keyAsColumn instructions
                FlatteningInstructions.KeyAsColumn keyAsColumn = instructions.getKeyAsColumn();
                JsonNode sourceObject = rootNode; // Default to root
                if (keyAsColumn.getSourcePath() != null && !keyAsColumn.getSourcePath().equals("$")) {
                    List<JsonNode> sourceNodes = JsonPath.using(jsonPathConfig).parse(rootNode)
                            .read(keyAsColumn.getSourcePath());
                    if (sourceNodes != null && !sourceNodes.isEmpty()) {
                        sourceObject = sourceNodes.get(0); // Assuming sourcePath points to a single object
                    }
                }

                Iterator<Map.Entry<String, JsonNode>> fields = sourceObject.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    Row row = new Row(schema.size());
                    populateFields(rootNode, targetNode, row, entry.getKey());
                    resultRows.add(row);
                }
            } else {
                // Process the targetNode as a regular item
                Row row = new Row(schema.size());
                populateFields(rootNode, targetNode, row);
                resultRows.add(row);
            }
        }

        return resultRows;
    }

    // Overloaded populateFields for general use
    private void populateFields(JsonNode rootNode, JsonNode currentNode, Row row) {
        populateFields(rootNode, currentNode, row, null);
    }

    // Main populateFields method with optional pivotedKey
    private void populateFields(JsonNode rootNode, JsonNode currentNode, Row row, String pivotedKey) {
        for (String fieldName : orderedFieldNames) {
            Object value = null;
            boolean fieldPopulated = false;

            // 1. Try to populate from pivotedKey if available and matches a schema field
            if (pivotedKey != null && fieldName.equals(instructions.getPivot().getKeyColumnName())) {
                value = pivotedKey;
                fieldPopulated = true;
            } else if (pivotedKey != null && instructions.getKeyAsColumn() != null && fieldName.equals(instructions.getKeyAsColumn().getName())) {
                value = pivotedKey;
                fieldPopulated = true;
            }

            // 2. Try to populate from target node (currentNode) if not already populated
            if (!fieldPopulated && currentNode != null) {
                if (instructions.getSelectFields() != null && !instructions.getSelectFields().isEmpty()) {
                    for (FlatteningInstructions.SelectField selectField : instructions.getSelectFields()) {
                        if (selectField.getName().equals(fieldName)) {
                            JsonNode node = JsonPath.using(jsonPathConfig).parse(currentNode)
                                    .read(selectField.getPath());
                            value = handleField(node, selectField.getArrayHandling(), selectField.getObjectHandling(), schema.get(fieldName));
                            fieldPopulated = true;
                            break;
                        }
                    }
                } else {
                    // Default behavior if no selectFields: flatten all fields from currentNode
                    if (currentNode.isObject() && currentNode.has(fieldName)) {
                        JsonNode node = currentNode.get(fieldName);
                        value = handleField(node, instructions.getArrayHandling(), instructions.getObjectHandling(), schema.get(fieldName));
                        fieldPopulated = true;
                    } else if (currentNode.isArray() && fieldName.equals("item")) { // Heuristic for array target
                        value = currentNode.toString(); // Put the whole array as JSON string
                        fieldPopulated = true;
                    } else if (!currentNode.isContainerNode() && fieldName.equals("value")) { // Heuristic for simple value target
                        value = convertJsonNode(currentNode, schema.get(fieldName));
                        fieldPopulated = true;
                    }
                }
            }

            // 3. If not populated from target, try to populate from parent node (rootNode)
            if (!fieldPopulated) {
                Object includeParentFields = instructions.getIncludeParentFields();
                if (includeParentFields != null) {
                    if (includeParentFields.equals("all")) {
                        // Try to read from rootNode if the field is not a target field
                        JsonNode node = JsonPath.using(jsonPathConfig).parse(rootNode).read("$." + fieldName);
                        if (node != null) {
                            value = convertJsonNode(node, schema.get(fieldName));
                            fieldPopulated = true;
                        }
                    } else if (includeParentFields instanceof List) {
                        List<String> parentPaths = (List<String>) includeParentFields;
                        for (String path : parentPaths) {
                            String parentFieldName = path.substring(path.lastIndexOf('.') + 1); // Extract field name from path
                            if (parentFieldName.equals(fieldName)) {
                                JsonNode node = JsonPath.using(jsonPathConfig).parse(rootNode).read(path);
                                if (node != null) {
                                    value = convertJsonNode(node, schema.get(fieldName));
                                    fieldPopulated = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Set the field in the row at its pre-calculated index
            if (fieldNameToIndexMap.containsKey(fieldName)) {
                row.setField(fieldNameToIndexMap.get(fieldName), value);
            } else {
                // This case indicates a mismatch between JSON data and schema, or an unhandled flattening scenario.
                // For now, it will be ignored, but consider logging a warning or throwing an exception.
            }
        }
    }

    private Object handleField(JsonNode node, String arrayHandling, String objectHandling, String targetDataType) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            // If it's an array and contains only one element, unwrap it
            if (node.size() == 1) {
                node = node.get(0);
                // If the unwrapped node is still an array or object, recursively call handleField
                if (node.isArray() || node.isObject()) {
                    return handleField(node, arrayHandling, objectHandling, targetDataType);
                }
            } else {
                String effectiveArrayHandling = arrayHandling != null ? arrayHandling
                        : (instructions.getArrayHandling() != null ? instructions.getArrayHandling() : "json");
                switch (effectiveArrayHandling) {
                    case "explode":
                        // This case should ideally be handled by the outer loop (parse method)
                        // For now, treat as json if it's a nested array not meant for explosion at this level
                        return node.toString();
                    case "ignore":
                        return null;
                    case "json":
                    default:
                        return node.toString();
                }
            }
        }

        if (node.isObject()) {
            String effectiveObjectHandling = objectHandling != null ? objectHandling
                    : (instructions.getObjectHandling() != null ? instructions.getObjectHandling() : "json");
            switch (effectiveObjectHandling) {
                case "flatten":
                    // Flattening requires dynamic schema, treat as json for now
                    return node.toString();
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
        	node = node.get(0);
        }
        switch (dataType.toLowerCase()) {
            case "string":
            	String asdf = node.asText();
                return asdf;
            case "integer":
                if (node.isNumber()) {
                    return node.asInt();
                } else {
                    return Integer.parseInt(node.asText());
                }
            case "long":
                if (node.isNumber()) {
                    return node.asLong();
                } else {
                    return Long.parseLong(node.asText());
                }
            case "double":
                if (node.isNumber()) {
                    return node.asDouble();
                } else {
                    return Double.parseDouble(node.asText());
                }
            case "boolean":
                if (node.isBoolean()) {
                    return node.asBoolean();
                } else {
                    return Boolean.parseBoolean(node.asText());
                }
            default:
                return node.asText(); // Default to string for unknown types
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
