package com.appulses;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
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
        String apiKey = properties.getProperty("apiKey");
        String apiSecret = properties.getProperty("apiSecret");
        String sectionId = properties.getProperty("sectionId");
        String baseUrl = properties.getProperty("baseUrl");
        String tokenUrl = properties.getProperty("tokenUrl");

        HttpClient client = HttpClient.newHttpClient();

        String token = null;
        try {
            String basic = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
            String response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basic)
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scopes=appulses.read+appulses.write"))
                    .build(), HttpResponse.BodyHandlers.ofString()).body();
            JSONObject tokenResponse = new JSONObject(response);
            token = tokenResponse.getString("access_token");
        } catch (IOException | InterruptedException | JSONException e) {
            logger.log(Level.WARNING,"Could not get token from appulses.com api");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/containers"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"My Service\",\"description\":\"My Service Description\", \"sectionId\": \"" + sectionId + "\"}"))
                .build();
        try {
           HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
              if (response.statusCode() != 200) {
                logger.log(Level.WARNING,"Could not add or update service in appulses.com api sectionId: "  + sectionId);
                return false;
              }
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

        String apiKey = appProperties.getProperty("apiKey");
        String apiSecret = appProperties.getProperty("apiSecret");
        String sectionId = appProperties.getProperty("sectionId");

        String yamlApiKey = appYamlProperties.getProperty("apiKey");
        String yamlApiSecret = appYamlProperties.getProperty("apiSecret");
        String yamlSectionId = appYamlProperties.getProperty("sectionId");

        if (apiKey != null && apiSecret != null) {
            appProperties.setProperty("apiKey", apiKey);
            appProperties.setProperty("apiSecret", apiSecret);
        } else if (yamlApiKey != null && yamlApiSecret != null) {
            appProperties.setProperty("apiKey", yamlApiKey);
            appProperties.setProperty("apiSecret", yamlApiSecret);
        } else {
            logger.log(Level.WARNING,"No apiKey or apiSecret found in properties");
            return appProperties;
        }
        if (sectionId != null) {
            appProperties.setProperty("sectionId", sectionId);
        } else if (yamlSectionId != null) {
            appProperties.setProperty("sectionId", yamlSectionId);
        } else {
            logger.log(Level.WARNING,"No sectionId found in properties");
            return appProperties;
        }
        String baseUrl = appYamlProperties.getProperty("baseUrl");
        String tokenUrl = appYamlProperties.getProperty("tokenUrl");
        appProperties.setProperty("baseUrl", baseUrl != null ? baseUrl : "https://appulses.com");
        appProperties.setProperty("tokenUrl", tokenUrl != null ? tokenUrl : "https://appulses.com/oauth2/token");
        return appProperties;
    }
}
