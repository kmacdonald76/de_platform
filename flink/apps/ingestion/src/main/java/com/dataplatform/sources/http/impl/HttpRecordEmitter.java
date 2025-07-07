package com.dataplatform.sources.http.impl;

import org.apache.flink.table.catalog.ResolvedSchema;
import com.dataplatform.JsonFlattener;
import com.dataplatform.sources.http.*;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HttpRecordEmitter implements RecordEmitter<byte[], Row, HttpSplit> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRecordEmitter.class);
    private final HttpSourceConfig config;
    private final JsonFlattener jsonFlattener;

    public HttpRecordEmitter(HttpSourceConfig config) {
        this.config = config;
        this.jsonFlattener = new JsonFlattener();
    }

    @Override
    public void emitRecord(byte[] rawData, SourceOutput<Row> output, HttpSplit split) {
        try {
            String jsonString = new String(rawData);

            if (config.getFlatteningConfig() != null && !config.getFlatteningConfig().isEmpty()) {
                List<Map<String, Object>> flattenedRows = jsonFlattener.flatten(jsonString,
                        config.getFlatteningConfig());
                ResolvedSchema flinkSchema = config.getFlinkSchema();

                for (Map<String, Object> flattenedRow : flattenedRows) {
                    List<String> fieldNames = flinkSchema.getColumnNames();
                    List<org.apache.flink.table.types.DataType> fieldTypes = flinkSchema.getColumnDataTypes();
                    Row row = new Row(fieldNames.size());
                    for (int i = 0; i < fieldNames.size(); i++) {
                        String fieldName = fieldNames.get(i);
                        row.setField(i, flattenedRow.get(fieldName));
                    }
                    output.collect(row);
                }
            } else {
                // Existing logic: use HttpRecordParser to parse byte[] to Row
                HttpRecordParser parser = HttpRecordParserFactory.fromConfig(config);
                List<Row> parsedRows = parser.parse(rawData);
                for (Row row : parsedRows) {
                    output.collect(row);
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing record for split {}: {}", split.splitId(), e.getMessage(), e);
            // Depending on error handling strategy, you might want to skip, log, or throw
        }
    }
}
