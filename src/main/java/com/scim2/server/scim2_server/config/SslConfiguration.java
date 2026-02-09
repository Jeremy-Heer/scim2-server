package com.scim2.server.scim2_server.config;

import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SSL Configuration for SCIM Server
 * Reads keystore password from certs/keystore.pin file and sets it as a system property
 */
@Configuration
public class SslConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SslConfiguration.class);
    private static final String KEYSTORE_PIN_FILE = "certs/keystore.pin";

    @PostConstruct
    public void loadKeystorePassword() {
        try {
            Path pinFilePath = Paths.get(KEYSTORE_PIN_FILE);
            if (Files.exists(pinFilePath)) {
                String password = Files.readString(pinFilePath).trim();
                System.setProperty("KEYSTORE_PASSWORD", password);
                logger.info("SSL keystore password loaded from {}", KEYSTORE_PIN_FILE);
            } else {
                logger.warn("Keystore PIN file not found at {}. Using default password from configuration.", KEYSTORE_PIN_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to read keystore PIN file: {}", e.getMessage());
            logger.warn("Falling back to default password from configuration");
        }
    }
}
