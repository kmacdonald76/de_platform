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

            for (Map.Entry<String, String> entry : data.entrySet()) {
                return new String(Base64.getDecoder().decode(entry.getValue()), StandardCharsets.UTF_8);
            }

        } catch (KubernetesClientException e) {
            LOG.error(e.getStackTrace().toString());
        }

        return null;
    }

    public static HashMap<String, String> getValueAsHashMap(String secretName, String namespace) {

        String yamlString = getValue(secretName, namespace);

        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        HashMap<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }

        return result;
    }
}

