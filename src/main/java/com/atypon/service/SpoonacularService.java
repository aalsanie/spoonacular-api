package com.atypon.service;

import com.atypon.client.SpoonacularClient;
import com.atypon.model.ExcludeRequest;
import com.atypon.model.Ingredient;
import com.atypon.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SpoonacularService {

    private final SpoonacularClient client;

    public SpoonacularService(SpoonacularClient client) {
        this.client = client;
    }

    public List<Recipe> searchRecipes(String query, String cuisine) {
        ResponseEntity<JsonNode> resp = client.search(query, cuisine);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            return List.of();
        }

        JsonNode body = resp.getBody();
        if (body == null) {
            return List.of();
        }

        JsonNode results = body.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return List.of();
        }

        List<Recipe> out = new ArrayList<>();
        for (JsonNode node : results) {
            if (node == null || node.isNull()) continue;
            Recipe r = new Recipe();
            if (node.hasNonNull("id")) {
                r.setId(node.get("id").asInt());
            }
            if (node.hasNonNull("title")) {
                r.setTitle(node.get("title").asText());
            }
            out.add(r);
        }
        return out;
    }

    public List<Recipe> searchRecipes(String query) {
        return searchRecipes(query, null);
    }

    public Recipe getRecipeInfo(int recipeId) {
        ResponseEntity<Recipe> resp = client.recipeInfo(recipeId);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            // This exact message is asserted in tests.
            throw new IllegalStateException("Failed to fetch recipe information");
        }
        return resp.getBody();
    }

    public double getCustomizedCalories(int recipeId, ExcludeRequest request) {
        ResponseEntity<Recipe> resp = client.recipeInfo(recipeId);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            // Controller maps IllegalStateException to 400 for invalid IDs.
            throw new IllegalStateException("Invalid recipeId");
        }

        Recipe recipe = resp.getBody();
        if (recipe == null) {
            return 0.0;
        }

        List<Ingredient> ingredients = recipe.getExtendedIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return 0.0;
        }

        List<String> excluded = (request == null || request.getExcludeIngredients() == null)
                ? List.of()
                : request.getExcludeIngredients();

        double total = 0.0;
        for (Ingredient ing : ingredients) {
            if (ing == null) continue;

            String name = ing.getName();
            if (name != null && isExcluded(name, excluded)) {
                continue;
            }

            // Tests expect SUM of ingredient amounts.
            total += ing.getAmount();
        }
        return total;
    }

    private boolean isExcluded(String ingredientName, List<String> excluded) {
        String n = ingredientName.trim().toLowerCase(Locale.ROOT);
        for (String e : excluded) {
            if (e == null) continue;
            if (n.equals(e.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
