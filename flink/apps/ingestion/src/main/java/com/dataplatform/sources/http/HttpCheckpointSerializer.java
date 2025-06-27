package com.dataplatform.sources.http;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import java.io.IOException;

public class HttpCheckpointSerializer implements SimpleVersionedSerializer<HttpCheckpoint> {

    public static final HttpCheckpointSerializer INSTANCE = new HttpCheckpointSerializer();

    public static final int CURRENT_VERSION = 1;

    @Override
    public int getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public byte[] serialize(HttpCheckpoint state) throws IOException {
        return new byte[0]; // No actual state to serialize
    }

    @Override
    public HttpCheckpoint deserialize(int version, byte[] serialized) throws IOException {
        return new HttpCheckpoint();
    }
}
