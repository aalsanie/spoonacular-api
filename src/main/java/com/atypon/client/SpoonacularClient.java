package com.atypon.client;

import com.atypon.config.SpoonacularConfig;
import com.atypon.model.Recipe;
import com.atypon.monitoring.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SpoonacularClient {

    private final RestTemplate restTemplate;
    private final SpoonacularConfig config;
    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    public SpoonacularClient(RestTemplate restTemplate, SpoonacularConfig config, ObjectMapper objectMapper, AlertService alertService) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
        this.alertService = alertService;
    }

    /**
     * Used by tests and service.
     */
    public ResponseEntity<JsonNode> search(String query, String cuisine) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl())
                    .path("/recipes/complexSearch")
                    .queryParam("query", query)
                    .queryParam("apiKey", config.getApiKey());

            if (cuisine != null && !cuisine.isBlank()) {
                b.queryParam("cuisine", cuisine);
            }

            URI uri = b.build().toUri();
            return exchangeJson(uri);
        }
        catch (Exception e) {
            alertService.alert("spoonacular.search.failed", "Failed to call Spoonacular search endpoint", e);
            throw e;
        }
    }

    public ResponseEntity<JsonNode> searchRecipes(String query) {
        return search(query, null);
    }

    public ResponseEntity<Recipe> recipeInfo(int recipeId) {
        return getRecipeInformation(recipeId, true);
    }

    public ResponseEntity<Recipe> getRecipeInformation(int recipeId, boolean includeNutrition) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl())
                    .path("/recipes/{id}/information")
                    .queryParam("includeNutrition", includeNutrition)
                    .queryParam("apiKey", config.getApiKey())
                    .buildAndExpand(Map.of("id", recipeId))
                    .toUri();

            return restTemplate.exchange(uri, HttpMethod.GET, null, Recipe.class);
        }
        catch (RestClientException e) {
            alertService.alert("spoonacular.recipeInfo.failed", "Failed to fetch recipe info from Spoonacular", e);
            throw e;
        }
    }

    private ResponseEntity<JsonNode> exchangeJson(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json");

        ResponseEntity<String> raw = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        try {
            JsonNode body = (raw.getBody() == null || raw.getBody().isBlank()) ? objectMapper.createObjectNode() : objectMapper.readTree(raw.getBody());
            return new ResponseEntity<>(body, raw.getHeaders(), raw.getStatusCode());
        }
        catch (Exception e) {
            alertService.alert("spoonacular.json.parse.failed", "Failed to parse Spoonacular JSON response", e);
            throw new IllegalStateException("Failed to parse Spoonacular JSON response", e);
        }
    }
}
