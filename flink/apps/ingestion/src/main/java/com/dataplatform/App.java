package com.dataplatform;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.catalog.CatalogDescriptor;

import com.dataplatform.utils.ConfigManager;

public class App {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(new Option("c", "config", true,
                "The name of the configuration that the application will use to ingest the source data."));
        options.addOption(new Option("o", "overwrite", false,
                "Overwrite existing table"));

        CommandLine commandLine = new DefaultParser().parse(options, args);
        String configSecretName = commandLine.getOptionValue("config");
        boolean overwrite = commandLine.hasOption("overwrite");

        IngestionConfig config = ConfigManager.setupClassConfig(configSecretName, IngestionConfig.class);
        Configuration flinkConfig = ConfigManager.setupFlinkConfig("services.flink.table-ingest");
        CatalogDescriptor catalogDesc = CatalogDescriptor.of("lakehouse", flinkConfig);

        // some sources require the data stream API, so inject that first
        TableEnvironment tableEnv = DataStreamInjector.inject(config);

        // otherwise create the typical TableEnv
        if (tableEnv == null) {
            tableEnv = Utils.generateTableEnv(config);
        }

        tableEnv.createCatalog("lakehouse", catalogDesc);

        new SourceStrategy().setupSource(tableEnv, config);
        new IcebergSinkStrategy().setupBronzeDestination(tableEnv, config, overwrite);
    }
}
