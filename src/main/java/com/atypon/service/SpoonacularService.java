package com.atypon.service;

import com.atypon.client.SpoonacularClient;
import com.atypon.model.ExcludeRequest;
import com.atypon.model.Ingredient;
import com.atypon.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpoonacularService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpoonacularService.class);

    private final SpoonacularClient client;

    public SpoonacularService(SpoonacularClient client) {
        this.client = client;
    }

    /**
     * @param query: represents the name of the recipe
     * @param cuisine: cuisine type - optional parameter
     * @return list of recipes matching passed parameter
     * **/
    public List<Recipe> searchRecipes(String query, String cuisine) {
        ResponseEntity<JsonNode> response = client.search(query, cuisine);
        if (response.getBody() == null || response.getBody().get("results") == null) {
            LOGGER.error("There are no results found for recipe {}", query);
            return List.of();
        }
        List<Recipe> recipes = new ArrayList<>();
        response.getBody().get("results").forEach(node -> {
            Recipe recipe = new Recipe();
            recipe.setId(node.get("id").asInt());
            recipe.setTitle(node.get("title").asText());
            recipes.add(recipe);
        });
        return recipes;
    }

    /**
     * @param recipeId: a unique identifier of the recipe
     * @return a recipe matching passed recipeId
     * **/
    public Recipe getRecipeInfo(int recipeId) {
        ResponseEntity<Recipe> response = client.recipeInfo(recipeId);

        if(response.getBody() == null){
            LOGGER.error("Failed to fetch information for recipeId {}", recipeId);
            throw new IllegalStateException("Failed to fetch recipe information");
        }
        return response.getBody();
    }

    /**
     * @param recipeId: a unique identifier of the recipe
     * @param excludeRequest: a list of ingredients to be excluded from calories calculation
     * @return number of calories
     * **/
    public double getCustomizedCalories(int recipeId, ExcludeRequest excludeRequest) {
        Recipe recipe = getRecipeInfo(recipeId);
        if (recipe.getExtendedIngredients() == null || recipe.getExtendedIngredients().isEmpty()) {
            return 0;
        }

        List<String> excluded = excludeRequest == null ? null : excludeRequest.getExcludeIngredients();

        double sum = 0;
        for (Ingredient ingredient : recipe.getExtendedIngredients()) {
            final String currentIngredient = ingredient.getName();
            if (excluded == null || !excluded.contains(currentIngredient)) {
               sum += ingredient.getAmount();
            }
        }
        return sum;
    }

}

