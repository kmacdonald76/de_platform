package com.dataplatform;

import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;

public class Utils {

    public static TableEnvironment generateTableEnv(IngestionConfig config) {

        TableEnvironment tableEnv;
        if (config.getProcessing().getMode().toLowerCase().contains("stream")) {
            tableEnv = TableEnvironment.create(
                    EnvironmentSettings.newInstance()
                            .inStreamingMode()
                            .build());
        } else {
            tableEnv = TableEnvironment.create(
                    EnvironmentSettings.newInstance()
                            .inBatchMode()
                            .build());
        }

        return tableEnv;

    }

}
