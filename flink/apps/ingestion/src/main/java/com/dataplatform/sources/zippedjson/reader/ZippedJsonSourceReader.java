package com.dataplatform.sources.zippedjson.reader;

import com.dataplatform.sources.zippedjson.*;
import com.dataplatform.sources.zippedjson.impl.*;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.types.RowKind;

import org.apache.flink.table.data.StringData;

import org.apache.flink.connector.base.source.reader.SingleThreadMultiplexSourceReaderBase;
import org.apache.flink.api.connector.source.SourceReaderContext;
import java.util.Map;

public class ZippedJsonSourceReader<OUT>
        extends SingleThreadMultiplexSourceReaderBase<String, OUT, ZippedJsonSplit, ZippedJsonSplit> {

    public ZippedJsonSourceReader(
            SourceReaderContext context,
            long batchSize) {

        // no idea how to make this function type safe.. this source will only support
        // RowData
        super(
                () -> new ZippedJsonSplitReader(batchSize),
                new ZippedJsonRecordEmitter<>((fileName, row) -> (OUT) convertToRowData(fileName, row)),
                context.getConfiguration(),
                context);
    }

    private static GenericRowData convertToRowData(String jsonFileName, String jsonLine) {
        GenericRowData row = new GenericRowData(RowKind.INSERT, 2);
        row.setField(0, StringData.fromString(jsonFileName));
        row.setField(1, StringData.fromString(jsonLine));
        return row;
    }

    @Override
    public void start() {
        if (getNumberOfCurrentlyAssignedSplits() == 0) {
            context.sendSplitRequest();
        }
    }

    @Override
    protected void onSplitFinished(Map<String, ZippedJsonSplit> finishedSplitIds) {
        context.sendSplitRequest();
    }

    @Override
    protected ZippedJsonSplit initializedState(ZippedJsonSplit split) {
        return split;
    }

    @Override
    protected ZippedJsonSplit toSplitType(String splitId, ZippedJsonSplit split) {
        return split;
    }

    @Override
    public void close() throws Exception {
    }
}
