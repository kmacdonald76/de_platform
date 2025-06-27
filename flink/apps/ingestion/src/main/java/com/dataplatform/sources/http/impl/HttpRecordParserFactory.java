package com.dataplatform.sources.http.impl;

import com.dataplatform.sources.http.HttpSourceConfig;

public class HttpRecordParserFactory {

    public static HttpRecordParser fromConfig(HttpSourceConfig config) throws Exception {

        switch (config.getSourceFormat()) {

            case "csv":
                return new CsvParser(config.getSchema(), config.getParserOptions());

            case "jsonl":
                return new NewlineJsonParser(config.getSchema());

            case "json_array":
                return new ArrayJsonParser(config.getSchema());

            case "json_array_field":
                return new ArrayFieldJsonParser(config.getSchema(), config.getArrayField());

            case "json_map_field":
                return new MapFieldJsonParser(config.getSchema(), config.getMapField());

            default:
                throw new IllegalArgumentException("Unsupported parser type: " + config.getSourceFormat());

        }

    }

}
