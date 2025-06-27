package com.dataplatform.formatters;

import org.apache.flink.types.RowKind;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.functions.FlatMapFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringToIcebergBronze implements FlatMapFunction<String, RowData> {

    private String fileName;
    private static final Logger LOG = LoggerFactory.getLogger(StringToIcebergBronze.class);

    public StringToIcebergBronze(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void flatMap(String value, Collector<RowData> out) {

        GenericRowData row = new GenericRowData(RowKind.INSERT, 2);
        row.setField(0, StringData.fromString(fileName));
        row.setField(1, StringData.fromString(value));

        out.collect(row);

    }

}
