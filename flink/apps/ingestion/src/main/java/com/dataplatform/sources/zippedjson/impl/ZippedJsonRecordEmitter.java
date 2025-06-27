package com.dataplatform.sources.zippedjson.impl;

import com.dataplatform.sources.zippedjson.*;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;

import java.util.function.BiFunction;

public class ZippedJsonRecordEmitter<OUT> implements RecordEmitter<String, OUT, ZippedJsonSplit> {

    private final BiFunction<String, String, OUT> map;

    public ZippedJsonRecordEmitter(BiFunction<String, String, OUT> map) {
        this.map = map;
    }

    @Override
    public void emitRecord(String row, SourceOutput<OUT> output, ZippedJsonSplit split) {
        OUT result = map.apply(split.jsonPath().toString(), row);
        output.collect(result);
    }
}
