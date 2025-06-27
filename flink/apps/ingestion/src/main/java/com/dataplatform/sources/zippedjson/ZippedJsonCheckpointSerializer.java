package com.dataplatform.sources.zippedjson;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import java.io.IOException;

public class ZippedJsonCheckpointSerializer implements SimpleVersionedSerializer<ZippedJsonCheckpoint> {

    public static final ZippedJsonCheckpointSerializer INSTANCE = new ZippedJsonCheckpointSerializer();

    public static final int CURRENT_VERSION = 1;

    @Override
    public int getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public byte[] serialize(ZippedJsonCheckpoint state) throws IOException {
        return new byte[0]; // No actual state to serialize
    }

    @Override
    public ZippedJsonCheckpoint deserialize(int version, byte[] serialized) throws IOException {
        return new ZippedJsonCheckpoint();
    }
}


