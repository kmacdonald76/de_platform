package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.flink.types.Row;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.csv.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface HttpRecordParser {
    List<Row> parse(byte[] responseBody) throws Exception;
}

class CsvParser implements HttpRecordParser {

    private Map<String, String> schema;
    private Map<String, String> parserOptions;
    private static final Logger LOG = LoggerFactory.getLogger(CsvParser.class);

    public CsvParser(Map<String, String> schema, Map<String, String> parserOptions) {
        this.schema = schema;
        this.parserOptions = parserOptions;
    }

    @Override
    public List<Row> parse(byte[] responseBody) throws Exception {

        // TODO: Provide packaging support (gzip, zip, etc)

        String content = new String(responseBody, StandardCharsets.UTF_8);
        Reader reader = new StringReader(content);
        CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder();

        String skipHeaderTest = parserOptions.getOrDefault("skipHeader", "false").toLowerCase();

        // Two most common options - could easily support more here
        if (parserOptions.getOrDefault("skipHeader", "false").toLowerCase().equals("true")) {
            formatBuilder.setHeader();
            formatBuilder.setSkipHeaderRecord(true);
        }

        if (parserOptions.containsKey("delimiter")) {
            formatBuilder.setDelimiter(parserOptions.get("delimiter"));
        }

        CSVParser parser = new CSVParser(reader, formatBuilder.build());

        List<Row> rows = new ArrayList<>();

        int schemaSize = schema.keySet().size();

        for (CSVRecord record : parser) {
            Row row = Row.withNames();
            int recordSize = record.size();

            if (recordSize != schemaSize) {
                // Two ways to handle this:
                // 1. skip the row (currently implemented)
                // 2. fill the rest of fields with null values
                // Consider building a config option for handling this..
                continue;
            }

            int idx = 0;
            for (Map.Entry<String, String> entry : schema.entrySet()) {
                String columnName = entry.getKey();
                String dataType = entry.getValue();
                String rawValue = record.get(idx++);
                row.setField(columnName, CsvTypeConverter.parseValue(rawValue, dataType));
            }

            rows.add(row);
        }

        parser.close();

        return rows;
    }
}

class NewlineJsonParser implements HttpRecordParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> schema;

    private static final Logger LOG = LoggerFactory.getLogger(ArrayJsonParser.class);

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

                List<JsonNode> zzz = rootNode.findValues(columnName);

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

class ArrayJsonParser implements HttpRecordParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> schema;

    private static final Logger LOG = LoggerFactory.getLogger(ArrayJsonParser.class);

    public ArrayJsonParser(Map<String, String> schema) {
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

            for (Map.Entry<String, String> entry : schema.entrySet()) {
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

class ArrayFieldJsonParser implements HttpRecordParser {

    private final String arrayField;
    private final Map<String, String> schema;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(ArrayFieldJsonParser.class);

    public ArrayFieldJsonParser(Map<String, String> schema, String arrayField) {
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

class MapFieldJsonParser implements HttpRecordParser {

    private final String mapField;
    private final Map<String, String> schema;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(ArrayFieldJsonParser.class);

    public MapFieldJsonParser(Map<String, String> schema, String mapField) {
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

            for (Map.Entry<String, String> schemaEntry : schema.entrySet()) {
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
