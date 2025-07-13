package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.flink.types.Row;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.dataplatform.model.Schema;

public class MapFieldJsonParser implements HttpRecordParser {

    private final String mapField;
    private final Schema schema;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(MapFieldJsonParser.class);

    public MapFieldJsonParser(Schema schema, String mapField) {
        this.mapField = mapField;
        this.schema = schema;
    }

    @Override
    public List<Row> parse(byte[] responseBody) throws Exception {

        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode mapNode = root.get(mapField);

        List<Row> rows = new ArrayList<>();

        if (mapNode == null || !mapNode.isObject()) {
            throw new Exception(String.format("Map field \"%s\" is not a map", mapField));
        }

        ObjectNode objectNode = (ObjectNode) mapNode;
        Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();

        while (iter.hasNext()) {
            Row row = Row.withNames();
            Map.Entry<String, JsonNode> jsonEntry = iter.next();

            for (Map.Entry<String, String> schemaEntry : schema.getFields().entrySet()) {
                String columnName = schemaEntry.getKey();
                String dataType = schemaEntry.getValue();

                if (columnName.equals(mapField)) {
                    row.setField(columnName, jsonEntry.toString());
                } else {
                    JsonNode fieldNode = root.get(columnName);
                    if (fieldNode == null) {
                        throw new Exception(String.format("Field \"%s\" is not found in the JSON: %s", columnName,
                                root.toString()));
                    }
                    row.setField(columnName, JsonTypeConverter.convertJsonNodeToFlinkType(fieldNode, dataType));
                }
            }

            rows.add(row);
        }

        return rows;
    }
}
