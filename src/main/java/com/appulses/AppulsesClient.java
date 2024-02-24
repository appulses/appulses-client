package com.appulses;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppulsesClient {
    private final Logger logger = Logger.getLogger(AppulsesClient.class.getName());
    public void addOrUpdate() {
        if (updated()) {
            logger.log(Level.INFO,"Service added or updated successfully");
        } else {
            logger.log(Level.WARNING,"Failed to add or update service in appulses.com api");
        }
    }

    private boolean updated() {
        Properties properties = getProperties();
        if (properties.isEmpty()) {
            return false;
        }
        String apiKey = properties.getProperty("appulses.apiKey");
        String apiSecret = properties.getProperty("appulses.apiSecret");
        String sectionId = properties.getProperty("appulses.sectionId");
        String baseUrl = properties.getProperty("appulses.baseUrl");

        HttpClient client = HttpClient.newHttpClient();

        String token = null;
        try {
            token = client.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + apiKey + ":" + apiSecret)
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scopes=appulses.read+appulses.write"))
                    .build(), HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING,"Could not get token from appulses.com api");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"My Service\",\"description\":\"My Service Description\", \"sectionId\": \"" + sectionId + "\"}"))
                .build();
        try {
            client.send(request, null);
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING,"Could not add or update service in appulses.com api sectionId: "  + sectionId);
        }
        return true;
    }

    private Properties getProperties() {
        String profile = System.getProperty("spring.profiles.active");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String appPropertiesFileName = "application.properties";
        String appPropertiesYamlFileName = "application.yaml";

        if ("default".equals(profile)) {
            appPropertiesFileName =  "application-" + profile + ".properties";
            appPropertiesYamlFileName = "application-" + profile + ".yaml";
        }

        Properties appProperties = new Properties();
        Properties appYamlProperties = new Properties();


        try (InputStream resourceStream = loader.getResourceAsStream(appPropertiesFileName)) {
            if (resourceStream != null) {
                appProperties.load(resourceStream);
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "No properties "+ appPropertiesFileName +" found");
        }

        try (InputStream resourceStream = loader.getResourceAsStream(appPropertiesYamlFileName)) {
            if (resourceStream != null) {
                appYamlProperties.load(resourceStream);
            }
        } catch (IOException e) {
            logger.log(Level.INFO,"No properties "+ appPropertiesFileName +" found");
        }

        if (appProperties.isEmpty() && appYamlProperties.isEmpty()) {
            logger.log(Level.WARNING,"No properties found. Please make sure you have application.properties or application.yaml in the resource directory");
            return appProperties;
        }

        String apiKey = appProperties.getProperty("appulses.apiKey");
        String apiSecret = appProperties.getProperty("appulses.apiSecret");

        String yamlApiKey = appYamlProperties.getProperty("appulses.apiKey");
        String yamlApiSecret = appYamlProperties.getProperty("appulses.apiSecret");

        if (apiKey != null && apiSecret != null) {
            appProperties.setProperty("appulses.apiKey", apiKey);
            appProperties.setProperty("appulses.apiSecret", apiSecret);
        } else if (yamlApiKey != null && yamlApiSecret != null) {
            appProperties.setProperty("appulses.apiKey", yamlApiKey);
            appProperties.setProperty("appulses.apiSecret", yamlApiSecret);
        } else {
            logger.log(Level.WARNING,"No apiKey or apiSecret found in properties");
            return appProperties;
        }
        return appProperties;
    }
}
