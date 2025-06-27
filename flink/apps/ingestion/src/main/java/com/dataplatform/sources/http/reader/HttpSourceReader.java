package com.dataplatform.sources.http.reader;

import com.dataplatform.sources.http.*;
import com.dataplatform.sources.http.impl.*;
import org.apache.flink.types.Row;

import org.apache.flink.connector.base.source.reader.SingleThreadMultiplexSourceReaderBase;
import org.apache.flink.api.connector.source.SourceReaderContext;
import java.util.Map;

public class HttpSourceReader<OUT>
        extends SingleThreadMultiplexSourceReaderBase<Row, OUT, HttpSplit, HttpSplit> {

    public HttpSourceReader(
            SourceReaderContext context) {
        super(
                () -> new HttpSplitReader(),
                new HttpRecordEmitter<>(row -> (OUT) row),
                context.getConfiguration(),
                context);
    }

    @Override
    public void start() {
        if (getNumberOfCurrentlyAssignedSplits() == 0) {
            context.sendSplitRequest();
        }
    }

    @Override
    protected void onSplitFinished(Map<String, HttpSplit> finishedSplitIds) {
        context.sendSplitRequest();
    }

    @Override
    protected HttpSplit initializedState(HttpSplit split) {
        return split;
    }

    @Override
    protected HttpSplit toSplitType(String splitId, HttpSplit split) {
        return split;
    }

    @Override
    public void close() throws Exception {
    }
}
