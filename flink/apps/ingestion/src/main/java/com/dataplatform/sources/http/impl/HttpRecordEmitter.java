package com.dataplatform.sources.http.impl;

import com.dataplatform.sources.http.*;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.types.Row;

import java.util.function.Function;

public class HttpRecordEmitter<OUT> implements RecordEmitter<Row, OUT, HttpSplit> {

    private final Function<Row, OUT> map;

    public HttpRecordEmitter(Function<Row, OUT> map) {
        this.map = map;
    }

    @Override
    public void emitRecord(Row row, SourceOutput<OUT> output, HttpSplit split) {
        OUT result = map.apply(row);
        output.collect(result);
    }
}
