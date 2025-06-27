package com.dataplatform;

import org.apache.flink.table.api.TableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SourceStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SourceStrategy.class);

    public void setupSource(TableEnvironment tableEnv, IngestionConfig config) throws Exception {

        if (config.getSource().getPackaging().trim().equalsIgnoreCase("zip_containing_jsons")) {
            // source already setup by the data stream injector
            return;
        }

        if (config.getSource().getType().equals("api")) {
            // source already setup by the data stream injector
            return;
        }

        StringBuilder options = new StringBuilder();

        if (config.getSource().getType().equals("s3")) {
            options.append("'connector' = 'filesystem',");
            options.append(String.format("'path' = '%s',", config.getSource().getPath()));
        }

        if (config.getSource().getFormat().equals("csv")) {
            options.append("'format' = 'csv',");
            options.append("'csv.ignore-parse-errors' = 'true',");
            options.append("'csv.allow-comments' = 'true',");
        }

        if (config.getSource().getFormat().equals("json")) {

            // Two ways to handle json:
            // 1. each first depth field is parsed as a column
            // 2. consume each line as one long string ('raw')
            //
            // The selected option depends on whether the user provided a column list
            // (option #1) or not (option #2)
            if (config.getSchema() != null && config.getSchema().size() > 0) {

                options.append("'format' = 'json',");

                if (config.getSource().getType().equals("s3")) {
                    options.append("'json.fail-on-missing-field' = 'false',");
                    options.append("'json.ignore-parse-errors' = 'true',");
                }

            } else {
                options.append("'format' = 'raw',");
            }
        }

        String ddlString = String.format(
                "CREATE TEMPORARY TABLE lakehouse.source.%s (\n" +
                        "  %s\n" +
                        ") WITH (\n" +
                        "%s" +
                        ")",
                config.getDestination().getTable(),
                config.getSchemaString(),
                options.toString().replaceAll(",$", ""));

        tableEnv.executeSql(ddlString);
        return;
    }
}
