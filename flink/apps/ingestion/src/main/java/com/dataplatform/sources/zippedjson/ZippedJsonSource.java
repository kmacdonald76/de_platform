package com.dataplatform.sources.zippedjson;

import com.dataplatform.sources.zippedjson.enumerate.*;
import com.dataplatform.sources.zippedjson.reader.*;

import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.annotation.Internal;

import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import org.apache.flink.core.fs.Path;

public class ZippedJsonSource<OUT>
        implements Source<OUT, ZippedJsonSplit, ZippedJsonCheckpoint>, ResultTypeQueryable<OUT> {

    private long batchSize; // num of lines allowed in a json before it's chunked into multiple splits
    private Path zipPath;
    private Class<OUT> pojoClass;

    public ZippedJsonSource(
            Path zipPath, Class<OUT> pojoClass, long batchSize) {
        this.zipPath = zipPath;
        this.pojoClass = pojoClass;
        this.batchSize = batchSize;
    }

    public ZippedJsonSource(
            Path zipPath, Class<OUT> pojoClass) {
        this(zipPath, pojoClass, 100000);
    }

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.BOUNDED;
    }

    @Internal
    @Override
    public SourceReader<OUT, ZippedJsonSplit> createReader(SourceReaderContext readerContext) {
        return new ZippedJsonSourceReader<OUT>(readerContext, batchSize);
    }

    @Override
    public SplitEnumerator<ZippedJsonSplit, ZippedJsonCheckpoint> createEnumerator(
            SplitEnumeratorContext<ZippedJsonSplit> enumContext) {
        return new ZippedJsonSplitEnumerator(enumContext, zipPath, batchSize);
    }

    @Override
    public SplitEnumerator<ZippedJsonSplit, ZippedJsonCheckpoint> restoreEnumerator(
            SplitEnumeratorContext<ZippedJsonSplit> enumContext, ZippedJsonCheckpoint enumCheckpoint) {
        return new ZippedJsonSplitEnumerator(enumContext, zipPath, batchSize);
    }

    @Override
    public SimpleVersionedSerializer<ZippedJsonSplit> getSplitSerializer() {
        return ZippedJsonSplitSerializer.INSTANCE;
    }

    @Override
    public SimpleVersionedSerializer<ZippedJsonCheckpoint> getEnumeratorCheckpointSerializer() {
        return new ZippedJsonCheckpointSerializer();
    }

    @Override
    public TypeInformation<OUT> getProducedType() {
        return TypeInformation.of(pojoClass);
    }
}
