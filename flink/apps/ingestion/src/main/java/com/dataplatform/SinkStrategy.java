package com.dataplatform;

import org.apache.flink.table.api.TableEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface SinkStrategy {
    void setupBronzeDestination(TableEnvironment tableEnv, IngestionConfig config, boolean overwrite);
}

class IcebergSinkStrategy implements SinkStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SourceStrategy.class);

    @Override
    public void setupBronzeDestination(TableEnvironment tableEnv, IngestionConfig config, boolean overwrite) {

        String ddlString;
        if (overwrite) {
            ddlString = String.format("DROP TABLE IF EXISTS lakehouse.bronze.%s", config.getDestination().getTable());
            tableEnv.executeSql(ddlString);
        }

        ddlString = String.format(
                "CREATE TABLE IF NOT EXISTS lakehouse.bronze.%s (\n" +
                        "  %s,\n" +
                        "  ingestion_time BIGINT,\n" +
                        "  metadata_enrichment STRING\n" +
                        ") WITH (\n" +
                        "  'format-version' = '2'\n" +
                        ")",
                config.getDestination().getTable(),
                config.getSchema().getSchemaString(),
                config.getDestination().getTable());

        tableEnv.executeSql(ddlString);

        ddlString = String.format(
                "INSERT INTO lakehouse.bronze.%s SELECT " +
                        "%s, " +
                        "(CAST(UNIX_TIMESTAMP() AS BIGINT) * 1000) + EXTRACT(MILLISECOND FROM CURRENT_TIMESTAMP), " +
                        "'%s' " +
                        "FROM lakehouse.source.%s",
                config.getDestination().getTable(),
                config.getSchema().getColumnList(),
                config.getMetadata().getEnrichmentFieldsAsJsonString(),
                config.getDestination().getTable());

        if (config.getSource() == null) {
            LOG.error("Source config is null");
            return;
        }

        LOG.info(ddlString);

        tableEnv.executeSql(ddlString);
    }
}
