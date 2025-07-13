package com.dataplatform.model;

import java.util.Map;

public class Schema {
    private Map<String, String> fields;

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public String getSchemaString() {
        if (fields == null || fields.isEmpty()) {
            return "item String";
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(" ")
                    .append(entry.getValue())
                    .append(",");
        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public String getColumnList() {
        if (fields == null || fields.isEmpty()) {
            return "item";
        }

        StringBuilder schemaBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            schemaBuilder.append(String.format("`%s`", entry.getKey()))
                    .append(",");
        }

        return schemaBuilder.substring(0, schemaBuilder.length() - 1);
    }

    public int size() {
        return fields.size();
    }
}
