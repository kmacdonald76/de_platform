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

            Source<Row, ?, ?> source = new ZippedJsonSource<Row>(new Path(config.getSource().getS3().getPath()),
                    Row.class);
            DataStream<Row> rowStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "ZippedJsonSource")
                    .setParallelism(1);

            Schema schema = datastreamSchemaConverter(config.getSchema().getFields());

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
            // TODO: Add iteration to API config
            // if (config.getSource().getIteration() != null) {
            // mechanism = config.getSource().getIteration().getMechanism();
            // parameters = config.getSource().getIteration().getParameters();
            // }

            // Create HttpSourceConfig from the ingestion config
            HttpSourceConfig httpConfig = new HttpSourceConfig(
                    config.getSource().getApi().getUrl(),
                    mechanism,
                    parameters,
                    config.getSource().getFormat(),
                    config.getSchema(),
                    config.getSource().getParserOptions(),
                    config.getSource().getFlatteningInstructions());

            // Create the HttpSource
            Source<Row, ?, ?> source = new HttpSource<>(httpConfig, Row.class);
            DataStream<Row> rowStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "HttpSource")
                    .setParallelism(1);

            Schema schema = datastreamSchemaConverter(config.getSchema().getFields());
            Table inputTable = streamTableEnv.fromDataStream(rowStream, schema);

            // Register the table
            streamTableEnv.createTemporaryView(
                    String.format("lakehouse.source.%s", config.getDestination().getTable()),
                    inputTable);

            return streamTableEnv;
        }

        return null;
    }

    // build the table schema dynamically based off the schema map provided in the
    // ingestion config
    private static Schema datastreamSchemaConverter(Map<String, String> sqlSchemaMap) throws Exception {

        List<DataTypes.Field> fieldList = new ArrayList<DataTypes.Field>();

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

        DataType rr = DataTypes.ROW(fieldList);

        Schema.Builder sb = Schema.newBuilder().column("f0", rr);
        for (String columnName : sqlSchemaMap.keySet()) {
            sb.columnByExpression(columnName, String.format("f0.`%s`", columnName));
        }

        return sb.build();

    }

}
