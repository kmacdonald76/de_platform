package com.dataplatform.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private static String deduceNamespace(String secretName) {

        String[] parts = secretName.split("\\.");
        String namespace;
        if (parts.length >= 2) {
            namespace = parts[1];
        } else {
            throw new IllegalArgumentException("Input must contain at least two parts separated by '.'");
        }

        return namespace;

    }

    public static org.apache.flink.configuration.Configuration setupFlinkConfig(String secretName) {

        String namespace = deduceNamespace(secretName);
        LOG.info("Setting up Flink config from secret '" + secretName + "' in namespace '" + namespace + "'");
        org.apache.flink.configuration.Configuration flinkConfig = new org.apache.flink.configuration.Configuration();
        HashMap<String, String> map = SecretReader.getMultiKeySecret(secretName, namespace);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            flinkConfig.setString(entry.getKey(), entry.getValue());
        }

        return flinkConfig;
    }

    public static <T> T setupClassConfig(String secretName, Class<T> configClass) throws IOException {
        String namespace = deduceNamespace(secretName);
        String yamlString = SecretReader.getValue(secretName, namespace);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(yamlString, configClass);
    }

}
