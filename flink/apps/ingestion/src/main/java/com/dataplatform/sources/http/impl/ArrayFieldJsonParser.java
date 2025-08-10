package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.flink.types.Row;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.dataplatform.model.Schema;

public class ArrayFieldJsonParser implements HttpRecordParser {

    private final String arrayField;
    private final Schema schema;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(ArrayFieldJsonParser.class);

    public ArrayFieldJsonParser(Schema schema, String arrayField) {
        this.arrayField = arrayField;
        this.schema = schema;
    }

    @Override
    public List<Row> parse(byte[] responseBody) throws Exception {

        JsonNode root = OBJECT_MAPPER.readTree(responseBody);

        JsonNode arrayNode;

        if (arrayField.indexOf(".") < 0) {

            LOG.info("KMDEBUG :: basic array field :: " + arrayField);
            arrayNode = root.get(arrayField);

        } else {

            LOG.info("KMDEBUG :: array field needs splits:: " + arrayField);
            String[] arrayNest = arrayField.split(".");
            LOG.info("KMDEBUG :: length:: " + arrayNest.length);

            int idx = 0;
            LOG.info("KMDEBUG :: firstNode:: " + arrayNest.length);
            JsonNode iter = root.get(arrayNest[0]);
            for (String val : arrayNest) {

                if (idx == 0) {
                    idx++;
                    continue;
                }

                iter = iter.get(val);

            }

            arrayNode = iter;
        }

        List<Row> rows = new ArrayList<>();

        if (arrayNode == null || !arrayNode.isArray()) {
            throw new Exception(String.format("Array field \"%s\" is not an array", arrayField));
        }

        for (JsonNode item : arrayNode) {
            Row row = Row.withNames();

            for (Map.Entry<String, String> entry : schema.entrySet()) {
                String columnName = entry.getKey();
                String dataType = entry.getValue();

                if (columnName.equals(arrayField)) {
                    row.setField(columnName, item.toString());
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
