package com.dataplatform.sources.zippedjson;

import org.apache.flink.api.connector.source.SourceSplit;
import java.io.Serializable;
import org.apache.flink.core.fs.Path;

public class ZippedJsonSplit implements SourceSplit, Serializable {

    private final String id;
    private final Path jsonFilePath;
    private final Path zipFilePath;
    private final long offset;

    public ZippedJsonSplit(
            Path zipFilePath,
            Path jsonFilePath,
            long offset) {

        this.id = zipFilePath.toString() + ":" + jsonFilePath.toString() + ":" + offset;
        this.zipFilePath = zipFilePath;
        this.jsonFilePath = jsonFilePath;
        this.offset = offset;
    }

    @Override
    public String splitId() {
        return id;
    }

    public Path jsonPath() {
        return jsonFilePath;
    }

    public Path zipPath() {
        return zipFilePath;
    }

    public long offset() {
        return offset;
    }

}
