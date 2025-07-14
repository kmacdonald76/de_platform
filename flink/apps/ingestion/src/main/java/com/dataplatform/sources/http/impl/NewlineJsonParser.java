package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.flink.types.Row;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NewlineJsonParser implements HttpRecordParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> schema;

    private static final Logger LOG = LoggerFactory.getLogger(NewlineJsonParser.class);

    public NewlineJsonParser(Map<String, String> schema) {
        this.schema = schema;
    }

    @Override
    public List<Row> parse(byte[] responseBody) throws Exception {

        List<Row> rows = new ArrayList<>();

        String content = new String(responseBody, StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            Row row = Row.withNames();
            JsonNode rootNode = objectMapper.readTree(line);

            for (Map.Entry<String, String> entry : schema.entrySet()) {
                String columnName = entry.getKey();
                String dataType = entry.getValue();

                JsonNode fieldNode = rootNode.get(columnName);

                if (fieldNode != null) {
                    Object test = JsonTypeConverter.convertJsonNodeToFlinkType(fieldNode, dataType);
                    row.setField(columnName, test);
                }
            }

            rows.add(row);
        }

        return rows;
    }
}
