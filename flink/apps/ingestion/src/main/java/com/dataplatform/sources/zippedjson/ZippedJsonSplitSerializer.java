package com.dataplatform.sources.zippedjson;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import java.io.IOException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;

import org.apache.flink.core.fs.Path;

public final class ZippedJsonSplitSerializer implements SimpleVersionedSerializer<ZippedJsonSplit> {

    public static final ZippedJsonSplitSerializer INSTANCE = new ZippedJsonSplitSerializer();

    public static final int CURRENT_VERSION = 1;

    private ZippedJsonSplitSerializer() {}

    @Override
    public int getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public byte[] serialize(ZippedJsonSplit split) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (DataOutputStream out = new DataOutputStream(baos)) {

                out.writeUTF(split.splitId());
                out.writeUTF(split.zipPath().toString());
                out.writeUTF(split.jsonPath().toString());
                out.writeLong(split.offset());

                return baos.toByteArray();
            }
        }
    }

    @Override
    public ZippedJsonSplit deserialize(int version, byte[] serialized) throws IOException {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized)) {
            try (DataInputStream in = new DataInputStream(bais)) {

                final String id = in.readUTF();
                Path zipFilePath = new Path(in.readUTF());
                Path jsonFilePath = new Path(in.readUTF());
                long offset = in.readLong();

                return new ZippedJsonSplit(
                        zipFilePath, 
                        jsonFilePath,
                        offset);
            }
        }

    }
}
