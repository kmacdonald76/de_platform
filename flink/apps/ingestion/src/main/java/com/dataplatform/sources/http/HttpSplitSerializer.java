package com.dataplatform.sources.http;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import java.io.IOException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.apache.flink.core.fs.Path;

public final class HttpSplitSerializer implements SimpleVersionedSerializer<HttpSplit> {

    public static final HttpSplitSerializer INSTANCE = new HttpSplitSerializer();

    public static final int CURRENT_VERSION = 1;

    private HttpSplitSerializer() {
    }

    @Override
    public int getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public byte[] serialize(HttpSplit split) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (DataOutputStream out = new DataOutputStream(baos)) {
                out.writeUTF(split.splitId());
                // Serialize the entire config as JSON
                out.writeUTF(split.config().toJson());
                return baos.toByteArray();
            }
        }
    }

    @Override
    public HttpSplit deserialize(int version, byte[] serialized) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized)) {
            try (DataInputStream in = new DataInputStream(bais)) {
                final String id = in.readUTF();
                // Deserialize the config from JSON
                final String configJson = in.readUTF();
                HttpSourceConfig config = new HttpSourceConfig(configJson);
                return new HttpSplit(id, config);
            }
        }
    }
}
