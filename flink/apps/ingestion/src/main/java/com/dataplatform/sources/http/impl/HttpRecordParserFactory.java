package com.dataplatform.sources.http.impl;

import com.dataplatform.sources.http.HttpSourceConfig;

public class HttpRecordParserFactory {

    public static HttpRecordParser fromConfig(HttpSourceConfig config) throws Exception {

        switch (config.getSourceFormat()) {

            case "csv":
                return new CsvParser(config.getSchema(), config.getParserOptions());

            case "jsonl":
                return new NewlineJsonParser(config.getSchema());

            case "json":
                return new JsonFlattenerParser(config.getSchema(), config.getFlatteningInstructions());

            default:
                throw new IllegalArgumentException("Unsupported parser type: " + config.getSourceFormat());

        }

    }

}
