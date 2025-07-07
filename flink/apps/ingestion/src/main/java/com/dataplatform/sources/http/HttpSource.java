package com.dataplatform.sources.http;

import com.dataplatform.sources.http.enumerate.*;
import com.dataplatform.sources.http.reader.*;

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

public class HttpSource
        implements Source<Row, HttpSplit, HttpCheckpoint>, ResultTypeQueryable<Row> {

    private HttpSourceConfig config;

    public HttpSource(
            HttpSourceConfig config) {
        this.config = config;
    }

    @Override
    public Boundedness getBoundedness() {

        if (config.getIterationMechanism() == null || config.getIterationMechanism().isBlank()) {
            return Boundedness.BOUNDED;
        }

        // time based iterations should be unbounded - but consider adding config option
        // to control this
        // processing.mode config also needs be set to streaming -
        // TODO: add check for processing mode
        if (config.getIterationMechanism().equals("date")) {
            return Boundedness.CONTINUOUS_UNBOUNDED;
        }

        return Boundedness.BOUNDED;

    }

    @Internal
    @Override
    public SourceReader<Row, HttpSplit> createReader(SourceReaderContext readerContext) {
        return new HttpSourceReader(readerContext, config);
    }

    @Override
    public SplitEnumerator<HttpSplit, HttpCheckpoint> createEnumerator(
            SplitEnumeratorContext<HttpSplit> enumContext) {
        return new HttpSplitEnumerator(enumContext, config, null);
    }

    @Override
    public SplitEnumerator<HttpSplit, HttpCheckpoint> restoreEnumerator(
            SplitEnumeratorContext<HttpSplit> enumContext, HttpCheckpoint enumCheckpoint) {
        return new HttpSplitEnumerator(enumContext, config, enumCheckpoint);
    }

    @Override
    public SimpleVersionedSerializer<HttpSplit> getSplitSerializer() {
        return HttpSplitSerializer.INSTANCE;
    }

    @Override
    public SimpleVersionedSerializer<HttpCheckpoint> getEnumeratorCheckpointSerializer() {
        return new HttpCheckpointSerializer();
    }

    @Override
    public TypeInformation<Row> getProducedType() {
        return TypeInformation.of(Row.class);
    }
}
