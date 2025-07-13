package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import org.apache.flink.types.Row;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.dataplatform.model.Schema;

public class ArrayJsonParser implements HttpRecordParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Schema schema;

    private static final Logger LOG = LoggerFactory.getLogger(ArrayJsonParser.class);

    public ArrayJsonParser(Schema schema) {
        this.schema = schema;
    }

    @Override
    public List<Row> parse(byte[] responseBody) throws Exception {
        List<Row> rows = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(responseBody);

        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("Expected JSON array but got: " + rootNode.getNodeType());
        }

        for (JsonNode element : rootNode) {
            Row row = Row.withNames();

            for (java.util.Map.Entry<String, String> entry : schema.getFields().entrySet()) {
                String columnName = entry.getKey();
                String dataType = entry.getValue();

                JsonNode fieldNode = element.get(columnName);
                if (fieldNode != null) {
                    row.setField(columnName, JsonTypeConverter.convertJsonNodeToFlinkType(fieldNode, dataType));
                }
            }
            rows.add(row);
        }

        return rows;
    }
}
