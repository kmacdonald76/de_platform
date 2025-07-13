package com.dataplatform.model;

import java.util.List;

public class Destination {
    private String layer;
    private String table;
    private String format;
    private List<Partition> partitioning;

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<Partition> getPartitioning() {
        return partitioning;
    }

    public void setPartitioning(List<Partition> partitioning) {
        this.partitioning = partitioning;
    }
}
