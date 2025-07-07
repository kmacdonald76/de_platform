package com.dataplatform;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataplatform.sources.http.HttpSource;
import com.dataplatform.sources.http.HttpSourceConfig;
import com.dataplatform.sources.zippedjson.ZippedJsonSource;

public class DataStreamInjector {

    private static final Logger LOG = LoggerFactory.getLogger(DataStreamInjector.class);

    public static TableEnvironment inject(IngestionConfig config) throws Exception {

        // currently the only data source that requires datastream API
        if (config.getSource().getPackaging().trim().equalsIgnoreCase("zip_containing_jsons")) {

            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            StreamTableEnvironment streamTableEnv = StreamTableEnvironment.create(env);

            Source<Row, ?, ?> source = new ZippedJsonSource<Row>(new Path(config.getSource().getPath()),
                    Row.class);
            DataStream<Row> rowStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "ZippedJsonSource")
                    .setParallelism(1);

            Schema schema = buildSchemaFromConfig(config.getSchema(), null);

            Table inputTable = streamTableEnv.fromDataStream(rowStream, schema);

            streamTableEnv.createTemporaryView(String.format("lakehouse.source.%s", config.getDestination().getTable()),
                    inputTable);

            return streamTableEnv;
        }

        if (config.getSource().getType().trim().equalsIgnoreCase("api")) {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            StreamTableEnvironment streamTableEnv = StreamTableEnvironment.create(env);

            String mechanism = null;
            Map<String, String> parameters = null;
            if (config.getSource().getIteration() != null) {
                mechanism = config.getSource().getIteration().getMechanism();
                parameters = config.getSource().getIteration().getParameters();
            }

            // Build the Flink Schema based on the ingestion config
            Schema schema = buildSchemaFromConfig(config.getSchema(), config.getFlatteningConfig());

            // Obtain ResolvedSchema from the Schema
            ResolvedSchema resolvedSchema = streamTableEnv.fromDataStream(env.fromElements(Row.of(1)), schema)
                    .getResolvedSchema();

            // Create HttpSourceConfig from the ingestion config
            HttpSourceConfig httpConfig = new HttpSourceConfig(
                    config.getSource().getConnection().getUrl(),
                    mechanism,
                    parameters,
                    config.getSource().getFormat(),
                    config.getSchema(),
                    config.getSource().getParserOptions(),
                    config.getSource().getArrayField(),
                    config.getSource().getMapField(),
                    config.getFlatteningConfig(),
                    resolvedSchema);

            // Create the HttpSource
            Source<Row, ?, ?> source = new HttpSource(httpConfig);
            DataStream<Row> rowStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "HttpSource")
                    .setParallelism(1);

            Table inputTable = streamTableEnv.fromDataStream(rowStream, schema);

            return streamTableEnv;
        }

        return null;
    }

    private static List<DataTypes.Field> extractSchemaFromFlatteningConfig(Map<String, Object> flatteningConfig)
            throws Exception {
        List<DataTypes.Field> fields = new ArrayList<>();
        if (flatteningConfig == null || flatteningConfig.isEmpty()) {
            return fields;
        }

        if (flatteningConfig.containsKey("f.map_merge_with_rows")) {
            Map<String, Object> mapConfig = (Map<String, Object>) flatteningConfig.get("f.map_merge_with_rows");
            for (Map.Entry<String, Object> entry : mapConfig.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (key.equals("f._parent_key")) {
                    fields.add(DataTypes.FIELD(key, convertStringToDataType((String) value)));
                } else if (value instanceof Map) {
                    // Recursive call for nested functions or schema definitions
                    fields.addAll(extractSchemaFromFlatteningConfig((Map<String, Object>) value));
                } else if (value instanceof String) {
                    // Direct field definition
                    fields.add(DataTypes.FIELD(key, convertStringToDataType((String) value)));
                }
            }
        } else if (flatteningConfig.containsKey("f.array_of_maps_to_rows")) {
            Map<String, Object> arrayConfig = (Map<String, Object>) flatteningConfig.get("f.array_of_maps_to_rows");
            for (Map.Entry<String, Object> entry : arrayConfig.entrySet()) {
                String arrayKey = entry.getKey();
                Map<String, Object> itemSchema = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> schemaEntry : itemSchema.entrySet()) {
                    String fieldName = schemaEntry.getKey();
                    String fieldType = (String) schemaEntry.getValue();
                    fields.add(DataTypes.FIELD(fieldName, convertStringToDataType(fieldType)));
                }
            }
        } else {
            // This case handles the direct schema definition within f.array_of_maps_to_rows
            // or any other direct schema definition.
            for (Map.Entry<String, Object> entry : flatteningConfig.entrySet()) {
                String fieldName = entry.getKey();
                String fieldType = (String) entry.getValue();
                fields.add(DataTypes.FIELD(fieldName, convertStringToDataType(fieldType)));
            }
        }
        return fields;
    }

    private static DataType convertStringToDataType(String sqlDataType) throws Exception {
        switch (sqlDataType.toLowerCase()) {
            case "string":
                return DataTypes.STRING();
            case "integer":
                return DataTypes.INT();
            case "long":
                return DataTypes.BIGINT();
            case "double":
                return DataTypes.DOUBLE();
            case "boolean":
                return DataTypes.BOOLEAN();
            case "array<string>":
                return DataTypes.ARRAY(DataTypes.STRING());
            case "array<integer>":
                return DataTypes.ARRAY(DataTypes.INT());
            case "array<long>":
                return DataTypes.ARRAY(DataTypes.BIGINT());
            case "array<double>":
                return DataTypes.ARRAY(DataTypes.DOUBLE());
            case "array<boolean>":
                return DataTypes.ARRAY(DataTypes.BOOLEAN());
            default:
                throw new Exception(String.format("Unknown data type (%s) - implementation needed.", sqlDataType));
        }
    }

    // build the table schema dynamically based off the schema map provided in the
    // ingestion config
    private static Schema buildSchemaFromConfig(Map<String, String> sqlSchemaMap, Map<String, Object> flatteningConfig)
            throws Exception {

        List<DataTypes.Field> fieldList = new ArrayList<DataTypes.Field>();

        if (flatteningConfig != null && !flatteningConfig.isEmpty()) {
            fieldList.addAll(extractSchemaFromFlatteningConfig(flatteningConfig));
        } else {
            for (Map.Entry<String, String> entry : sqlSchemaMap.entrySet()) {
                String columnName = entry.getKey();
                String sqlDataType = entry.getValue();

                DataTypes.Field dataType;

                switch (sqlDataType) {
                    case "STRING":
                    case "VARCHAR":
                    case "TEXT":
                        dataType = DataTypes.FIELD(columnName, DataTypes.STRING());
                        break;
                    case "LONG":
                    case "BIGINT":
                        dataType = DataTypes.FIELD(columnName, DataTypes.BIGINT());
                        break;
                    case "INT":
                    case "INTEGER":
                        dataType = DataTypes.FIELD(columnName, DataTypes.INT());
                        break;
                    case "DECIMAL":
                        dataType = DataTypes.FIELD(columnName, DataTypes.DECIMAL(38, 18));
                        break;
                    case "DOUBLE":
                        dataType = DataTypes.FIELD(columnName, DataTypes.DOUBLE());
                        break;
                    case "FLOAT":
                        dataType = DataTypes.FIELD(columnName, DataTypes.FLOAT());
                        break;
                    case "ARRAY<STRING>":
                        dataType = DataTypes.FIELD(columnName, DataTypes.ARRAY(DataTypes.STRING()));
                        break;
                    case "BOOL":
                    case "BOOLEAN":
                        dataType = DataTypes.FIELD(columnName, DataTypes.BOOLEAN());
                        break;
                    default:
                        throw new Exception(
                                String.format("Unknown data type (%s) - implementation needed.", sqlDataType));

                }

                fieldList.add(dataType);
            }
        }

        DataType rr = DataTypes.ROW(fieldList);

        Schema.Builder sb = Schema.newBuilder().column("f0", rr);
        for (DataTypes.Field field : fieldList) {
            sb.columnByExpression(field.getName(), String.format("f0.`%s`", field.getName()));
        }

        return sb.build();

    }

}
