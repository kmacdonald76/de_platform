package com.dataplatform.sources.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvTypeConverter {

    private static final Logger LOG = LoggerFactory.getLogger(CsvTypeConverter.class);

    public static Object parseValue(String value, String type, Boolean required) throws Exception {

        if (value == null || value.isEmpty()) {
            if (required)
                throw new Exception("Required field is missing");

            return null;
        }

        try {
            switch (type.toLowerCase()) {
                case "int":
                case "integer":
                    return Integer.parseInt(value);
                case "bigint":
                case "long":
                    return Long.parseLong(value);
                case "float":
                case "real":
                    return Float.parseFloat(value);
                case "decimal":
                case "double":
                    return Double.parseDouble(value);
                case "boolean":
                    return Boolean.parseBoolean(value);
                case "date":
                    return java.sql.Date.valueOf(value);
                case "timestamp":
                    return java.sql.Timestamp.valueOf(value); // must be yyyy-MM-dd HH:mm:ss[.f...]
                case "string":
                case "varchar":
                case "text":
                    return value;
                default:
                    throw new IllegalArgumentException("Unsupported type found for csv parser: " + type);
            }

            // TODO add more catches
        } catch (NumberFormatException e) {
            if (required) {
                throw new Exception("Required field failed to parse value: " + value);
            }
            return null;
        }
    }

    public static Object parseValue(String value, String type) throws Exception {
        return parseValue(value, type, false);
    }
}
