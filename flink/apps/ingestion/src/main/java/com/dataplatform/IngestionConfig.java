package com.dataplatform;

import com.dataplatform.model.Destination;
import com.dataplatform.model.Metadata;
import com.dataplatform.model.Processing;
import com.dataplatform.model.Source;
import com.dataplatform.model.Schema;

public class IngestionConfig {
    private Source source;
    private Destination destination;
    private Processing processing;
    private Metadata metadata;
    private Schema schema;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
}
