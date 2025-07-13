package com.dataplatform.sources.http.impl;

import com.dataplatform.model.FlatteningInstructions;
import com.dataplatform.model.Schema;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonFlattenerParser implements HttpRecordParser {

    private final Schema schema;
    private final FlatteningInstructions instructions;
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;

    public JsonFlattenerParser(Schema schema, FlatteningInstructions instructions) {
        this.schema = schema;
        this.instructions = instructions;
        this.objectMapper = new ObjectMapper();
        this.jsonPathConfig = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST)
                .build();
    }

    @Override
    public List<Row> parse(byte[] record) throws Exception {
        JsonNode rootNode = objectMapper.readTree(record);
        List<Row> resultRows = new ArrayList<>();

        // Get the target nodes based on the JSONPath
        List<JsonNode> targetNodes = JsonPath.using(jsonPathConfig).parse(rootNode.toString()).read(instructions.getTarget());

        if (targetNodes == null || targetNodes.isEmpty()) {
            // If target is not found or empty, and no specific selectFields, return an empty row if parent fields are included
            if (instructions.getIncludeParentFields() != null && !instructions.getIncludeParentFields().equals("none")) {
                Row emptyRow = new Row(schema.getFields().size());
                populateParentFields(rootNode, emptyRow, 0);
                resultRows.add(emptyRow);
            }
            return resultRows;
        }

        for (JsonNode targetNode : targetNodes) {
            if (targetNode.isArray()) {
                for (JsonNode element : targetNode) {
                    processNode(rootNode, element, resultRows);
                }
            } else if (targetNode.isObject()) {
                if (instructions.getPivot() != null) {
                    // Handle pivoting
                    FlatteningInstructions.Pivot pivot = instructions.getPivot();
                    Iterator<Map.Entry<String, JsonNode>> fields = targetNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        JsonNode pivotedValue = JsonPath.using(jsonPathConfig).parse(entry.getValue().toString()).read(pivot.getValuePath());
                        Row row = new Row(schema.getFields().size());
                        int currentIndex = populateParentFields(rootNode, row, 0);
                        row.setField(currentIndex++, entry.getKey()); // Add key as a column
                        populateFields(pivotedValue, row, currentIndex);
                        resultRows.add(row);
                    }
                } else if (instructions.getKeyAsColumn() != null) {
                    // Handle keyAsColumn
                    FlatteningInstructions.KeyAsColumn keyAsColumn = instructions.getKeyAsColumn();
                    JsonNode sourceObject = rootNode; // Default to root
                    if (keyAsColumn.getSourcePath() != null && !keyAsColumn.getSourcePath().equals("$")) {
                        List<JsonNode> sourceNodes = JsonPath.using(jsonPathConfig).parse(rootNode.toString()).read(keyAsColumn.getSourcePath());
                        if (sourceNodes != null && !sourceNodes.isEmpty()) {
                            sourceObject = sourceNodes.get(0); // Assuming sourcePath points to a single object
                        }
                    }

                    Iterator<Map.Entry<String, JsonNode>> fields = sourceObject.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        Row row = new Row(schema.getFields().size());
                        int currentIndex = populateParentFields(rootNode, row, 0);
                        row.setField(currentIndex++, entry.getKey()); // Add key as a column
                        populateFields(targetNode, row, currentIndex);
                        resultRows.add(row);
                    }
                } else {
                    // If target is an object and no pivot/keyAsColumn, treat it as a single row
                    processNode(rootNode, targetNode, resultRows);
                }
            } else {
                // If target is a single value, treat it as a single row
                processNode(rootNode, targetNode, resultRows);
            }
        }

        return resultRows;
    }

    private void processNode(JsonNode rootNode, JsonNode currentNode, List<Row> resultRows) {
        Row row = new Row(schema.getFields().size());
        int currentIndex = populateParentFields(rootNode, row, 0);
        populateFields(currentNode, row, currentIndex);
        resultRows.add(row);
    }

    private int populateParentFields(JsonNode rootNode, Row row, int startIndex) {
        int currentIndex = startIndex;
        Object includeParentFields = instructions.getIncludeParentFields();

        if (includeParentFields != null) {
            if (includeParentFields.equals("all")) {
                // Iterate through all fields in the root node and add them
                for (Map.Entry<String, String> fieldEntry : schema.getFields().entrySet()) {
                    String fieldName = fieldEntry.getKey();
                    String dataType = fieldEntry.getValue();
                    JsonNode node = JsonPath.using(jsonPathConfig).parse(rootNode.toString()).read("$." + fieldName);
                    if (node != null) {
                        row.setField(currentIndex++, convertJsonNode(node, dataType));
                    } else {
                        row.setField(currentIndex++, null);
                    }
                }
            } else if (includeParentFields instanceof List) {
                List<String> parentPaths = (List<String>) includeParentFields;
                for (String path : parentPaths) {
                    JsonNode node = JsonPath.using(jsonPathConfig).parse(rootNode.toString()).read(path);
                    if (node != null) {
                        // Need to determine the schema field for this path to get data type
                        // For now, just add as string or basic type
                        row.setField(currentIndex++, convertJsonNode(node, "string")); // Default to string
                    } else {
                        row.setField(currentIndex++, null);
                    }
                }
            }
        }
        return currentIndex;
    }

    private void populateFields(JsonNode currentNode, Row row, int startIndex) {
        int currentIndex = startIndex;

        if (instructions.getSelectFields() != null && !instructions.getSelectFields().isEmpty()) {
            for (FlatteningInstructions.SelectField selectField : instructions.getSelectFields()) {
                JsonNode node = JsonPath.using(jsonPathConfig).parse(currentNode.toString()).read(selectField.getPath());
                if (node != null) {
                    row.setField(currentIndex++, handleField(node, selectField.getArrayHandling(), selectField.getObjectHandling()));
                } else {
                    row.setField(currentIndex++, null);
                }
            }
        } else {
            // Default behavior if no selectFields are specified: flatten all fields
            if (currentNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = currentNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    row.setField(currentIndex++, handleField(entry.getValue(), instructions.getArrayHandling(), instructions.getObjectHandling()));
                }
            } else if (currentNode.isArray()) {
                // This case should ideally be handled by target: "$.items" which iterates over array elements
                // If a raw array is passed here, treat it based on global array handling
                row.setField(currentIndex++, handleField(currentNode, instructions.getArrayHandling(), instructions.getObjectHandling()));
            } else {
                row.setField(currentIndex++, handleField(currentNode, null, null)); // Simple value
            }
        }
    }

    private Object handleField(JsonNode node, String arrayHandling, String objectHandling) {
        if (node.isArray()) {
            String effectiveArrayHandling = arrayHandling != null ? arrayHandling : (instructions.getArrayHandling() != null ? instructions.getArrayHandling() : "json");
            switch (effectiveArrayHandling) {
                case "explode":
                    // This should be handled by the outer loop if target is an array
                    // If an array is encountered here, it means it's a nested array within a field
                    // For now, treat as json if explode is not applicable at this level
                    return node.toString();
                case "ignore":
                    return null;
                case "json":
                default:
                    return node.toString();
            }
        } else if (node.isObject()) {
            String effectiveObjectHandling = objectHandling != null ? objectHandling : (instructions.getObjectHandling() != null ? instructions.getObjectHandling() : "json");
            switch (effectiveObjectHandling) {
                case "flatten":
                    // Flattening nested objects into the current row requires dynamic schema modification
                    // For now, treat as json if flatten is not directly applicable here
                    return node.toString();
                case "ignore":
                    return null;
                case "json":
                default:
                    return node.toString();
            }
        } else {
            return convertJsonNode(node, "string"); // Convert primitive types
        }
    }

    private Object convertJsonNode(JsonNode node, String dataType) {
        if (node == null || node.isNull()) {
            return null;
        }
        switch (dataType.toLowerCase()) {
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
            default:
                return node.asText(); // Default to string for unknown types
        }
    }

    public RowTypeInfo getRowTypeInfo() {
        // This method needs to dynamically create RowTypeInfo based on the flattened schema
        // For now, it will use the provided schema, but this might need adjustment
        // if 'flatten' or 'explode' dynamically add columns.
        String[] fieldNames = schema.getFields().keySet().toArray(new String[0]);

        TypeInformation<?>[] fieldTypes = schema.getFields().values().stream()
                .map(dataType -> {
                    switch (dataType.toLowerCase()) {
                        case "string": return Types.STRING;
                        case "integer": return Types.INT;
                        case "long": return Types.LONG;
                        case "double": return Types.DOUBLE;
                        case "boolean": return Types.BOOLEAN;
                        default: return Types.STRING; // Default to string
                    }
                })
                .toArray(TypeInformation<?>[]::new);

        return new RowTypeInfo(fieldTypes, fieldNames);
    }
}
