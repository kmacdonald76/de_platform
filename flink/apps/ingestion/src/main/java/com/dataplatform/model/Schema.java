package com.dataplatform.model;

import java.util.HashMap;
import java.util.Map;

public class Schema extends HashMap<String, String> {

    public String getSchemaString() {
        if (this.isEmpty()) {
            throw new IllegalArgumentException("Schema cannot be empty. Please provide at least one field.");
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : this.entrySet()) {
            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(" ")
                    .append(entry.getValue())
                    .append(",");
        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public String getColumnList() {
        if (this.isEmpty()) {
            return ""; // Or throw an exception if an empty column list is not allowed
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : this.entrySet()) {
            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(",");
        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public int size() {
        return super.size();
    }
}