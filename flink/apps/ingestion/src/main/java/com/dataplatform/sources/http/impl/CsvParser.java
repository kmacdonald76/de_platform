package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.flink.types.Row;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.*;
import java.io.*;

import com.dataplatform.model.Schema;

public class CsvParser implements HttpRecordParser {

    private Schema schema;
    private Map<String, String> parserOptions;
    private static final Logger LOG = LoggerFactory.getLogger(CsvParser.class);

    public CsvParser(com.dataplatform.model.Schema schema, Map<String, String> parserOptions) {
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

        int schemaSize = schema.getFields().keySet().size();

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
            for (Map.Entry<String, String> entry : schema.getFields().entrySet()) {
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
