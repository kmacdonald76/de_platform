package com.dataplatform.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import java.util.HashMap;

public class SecretReader {

    private static final Logger LOG = LoggerFactory.getLogger(SecretReader.class);
    private static final ConfigBuilder configBuilder = new ConfigBuilder();

    public static String getValue(String secretName, String namespace) {

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(configBuilder.build()).build()) {

            Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();

            if (secret == null) {
                LOG.error("Secret " + secretName + " not found in namespace " + namespace);
                return null;
            }

            Map<String, String> data = secret.getData();

            if (data == null) {
                throw new RuntimeException(
                        "Secret " + secretName + " in namespace " + namespace + " contains no data.");
            }

            for (Map.Entry<String, String> entry : data.entrySet()) {
                return new String(Base64.getDecoder().decode(entry.getValue()), StandardCharsets.UTF_8);
            }

        } catch (KubernetesClientException e) {
            LOG.error(e.getStackTrace().toString());
        }

        return null;
    }

    public static HashMap<String, String> getMultiKeySecret(String secretName, String namespace) {

        HashMap<String, String> result = new HashMap<>();
        LOG.info("Reading service secret '" + secretName + "' from namespace '" + namespace + "'");

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(configBuilder.build()).build()) {

            Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();

            if (secret == null) {
                LOG.error("Secret " + secretName + " not found in namespace " + namespace);
                return result;
            }

            Map<String, String> data = secret.getData();

            if (data == null) {
                throw new RuntimeException(
                        "Secret " + secretName + " in namespace " + namespace + " contains no data.");
            }

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = entry.getKey();
                String value = new String(Base64.getDecoder().decode(entry.getValue()), StandardCharsets.UTF_8);
                result.put(key, value);
                LOG.debug("Loaded key '" + key + "' from secret '" + secretName + "'");
            }

        } catch (KubernetesClientException e) {
            LOG.error("Error reading secret " + secretName + " from namespace " + namespace, e);
        }
        return result;
    }
}
