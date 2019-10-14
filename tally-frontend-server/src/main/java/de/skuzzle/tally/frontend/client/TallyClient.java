package de.skuzzle.tally.frontend.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class TallyClient {

    private static final Logger logger = LoggerFactory.getLogger(TallyClient.class);

    private final RestTemplate restTemplate;
    private final RestTemplate restTemplateHealth;
    private final ObjectMapper objectMapper;

    public TallyClient(RestTemplate restTemplate, RestTemplate restTemplateHealth, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.restTemplateHealth = restTemplateHealth;
        this.objectMapper = objectMapper;
    }

    public boolean isHealthy() {
        try {
            restTemplateHealth.getForEntity("/actuator/health", Object.class);
            return true;
        } catch (final Exception e) {
            logger.error("Backend seems not be available", e);
            return false;
        }
    }

    public TallyResult<RestTallySheetsReponse> listTallySheets() {
        try {
            final ResponseEntity<RestTallySheetsReponse> response = restTemplate.getForEntity("/",
                    RestTallySheetsReponse.class);
            return TallyResult.success(response.getStatusCode(), response.getBody());
        } catch (final HttpStatusCodeException e) {
            logger.debug("HTTP error while calling backend 'GET /", e);
            return resultFromException(e);
        }
    }

    public TallyResult<RestTallyResponse> createNewTallySheet(String name) {
        Preconditions.checkArgument(name != null, "name must not be null");
        try {
            final ResponseEntity<RestTallyResponse> response = restTemplate.postForEntity("/{name}", null,
                    RestTallyResponse.class, name);
            return TallyResult.success(response.getStatusCode(), response.getBody());
        } catch (final HttpStatusCodeException e) {
            logger.debug("HTTP error while calling backend 'POST /{}", name, e);
            return resultFromException(e);
        }
    }

    public TallyResult<RestTallyResponse> getTallySheet(String publicKey) {
        Preconditions.checkArgument(publicKey != null, "publicKey must not be null");

        try {
            final ResponseEntity<RestTallyResponse> response = restTemplate.getForEntity("/{key}",
                    RestTallyResponse.class, publicKey);
            return TallyResult.success(response.getStatusCode(), response.getBody());
        } catch (final HttpStatusCodeException e) {
            logger.debug("HTTP error while calling backend 'GET /{}", publicKey, e);
            return resultFromException(e);
        }
    }

    public boolean increment(String adminKey, RestTallyIncrement increment) {
        Preconditions.checkArgument(adminKey != null, "adminKey must not be null");
        Preconditions.checkArgument(increment != null, "increment must not be null");
        try {
            restTemplate.postForEntity("/{key}/increment", increment, RestTallyResponse.class, adminKey);
            return true;
        } catch (final HttpStatusCodeException e) {
            logger.debug("HTTP error while calling backend 'POST /{}/increment", adminKey, e);
            return false;
        }
    }

    public boolean deleteTallySheet(String adminKey) {
        Preconditions.checkArgument(adminKey != null, "adminKey must not be null");
        try {
            restTemplate.delete("/{key}", adminKey);
            return true;
        } catch (final Exception e) {
            logger.error("Error deleting tally sheet with key '{}'", adminKey, e);
            return false;
        }
    }

    public boolean deleteIncrement(String adminKey, String incrementId) {
        Preconditions.checkArgument(adminKey != null, "adminKey must not be null");
        Preconditions.checkArgument(incrementId != null, "incrementId must not be null");
        try {
            restTemplate.delete("/{key}/increment/{id}", adminKey, incrementId);
            return true;
        } catch (final Exception e) {
            logger.error("Error deleting increment {} from sheet with key '{}'", incrementId, adminKey, e);
            return false;
        }
    }

    public boolean assignToCurrentUser(String adminKey) {
        Preconditions.checkArgument(adminKey != null, "adminKey must not be null");
        try {
            restTemplate.postForEntity("/{key}/assignToCurrentUser", null, Object.class, adminKey);
            return true;
        } catch (final Exception e) {
            logger.error("Error while assigning sheet with key '{}' to current user", e);
            return false;
        }
    }

    private <T> TallyResult<T> resultFromException(HttpStatusCodeException e) {
        final RestErrorMessage error = error(e.getResponseBodyAsString());
        return TallyResult.fail(e.getStatusCode(), error);
    }

    private RestErrorMessage error(String errorResponseBody) {
        try {
            return objectMapper.readValue(errorResponseBody, RestErrorMessage.class);
        } catch (final IOException e) {
            logger.error("Error while deserializing exception response: {}", errorResponseBody, e);
            return new RestErrorMessage(e.getMessage(), e.getClass().getName());
        }
    }

}
