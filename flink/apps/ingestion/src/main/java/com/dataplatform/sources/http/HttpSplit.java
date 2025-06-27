package com.dataplatform.sources.http;

import org.apache.flink.api.connector.source.SourceSplit;
import java.io.Serializable;

public class HttpSplit implements SourceSplit, Serializable {

    private final String id;
    private final String url;
    private final HttpSourceConfig config;

    public HttpSplit(
            String url,
            HttpSourceConfig config) {
        this.id = url;
        this.url = url;
        this.config = config;
    }

    @Override
    public String splitId() {
        return id;
    }

    public String url() {
        return url;
    }

    public HttpSourceConfig config() {
        return config;
    }

}
