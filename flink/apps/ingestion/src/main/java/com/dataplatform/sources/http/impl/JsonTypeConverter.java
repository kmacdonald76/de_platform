package com.dataplatform.sources.http.impl;

import org.apache.flink.table.data.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class JsonTypeConverter {
    public static Object convertJsonNodeToFlinkType(JsonNode node, String dataType) throws Exception {
        dataType = dataType.toUpperCase();
        switch (dataType) {
            case "BOOLEAN":
                return node.asBoolean();
            case "STRING":
            case "VARCHAR":
            case "CHAR":
                return node.toString();
            case "BINARY":
            case "VARBINARY":
            case "BYTES":
                return node.binaryValue();
            case "DECIMAL":
                return DecimalData.fromBigDecimal(node.decimalValue(), 38, 18);
            case "TINYINT":
                return (byte) node.asInt();
            case "SMALLINT":
                return (short) node.asInt();
            case "INT":
                return node.asInt();
            case "BIGINT":
                return node.asLong();
            case "FLOAT":
                return (float) node.asDouble();
            case "DOUBLE":
                return node.asDouble();
            case "DATE":
                return node.asInt();
            case "TIME":
                return node.asInt();
            case "TIMESTAMP":
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                return TimestampData.fromEpochMillis(node.asLong());
            case "INTERVAL YEAR TO MONTH":
                return node.asInt();
            case "INTERVAL DAY TO SECOND":
                return node.asLong();
            case "ARRAY":
                if (node.isArray()) {
                    String[] array = new String[node.size()];
                    for (int i = 0; i < node.size(); i++) {
                        array[i] = node.get(i).asText();
                    }
                    return StringData.fromString(String.join(",", array));
                }
                break;
            case "MAP":
            case "MULTISET":
                if (node.isObject()) {
                    Map<String, String> map = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        map.put(field.getKey(), field.getValue().asText());
                    }
                    return StringData.fromString(map.toString());
                }
                break;
            case "RAW":
                return RawValueData.fromObject(node.asText());
        }
        throw new Exception("Unsupported data type: " + dataType);
    }
}
