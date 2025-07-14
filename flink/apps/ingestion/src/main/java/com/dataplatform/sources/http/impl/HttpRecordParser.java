package com.dataplatform.sources.http.impl;

import org.apache.flink.types.Row;

import java.util.List;

public interface HttpRecordParser {
    List<Row> parse(byte[] responseBody) throws Exception;
}
