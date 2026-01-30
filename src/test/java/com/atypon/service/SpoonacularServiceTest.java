package com.atypon.service;

import com.atypon.client.SpoonacularClient;
import com.atypon.model.ExcludeRequest;
import com.atypon.model.Ingredient;
import com.atypon.model.Nutrition;
import com.atypon.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpoonacularServiceTest {

    @Mock
    private SpoonacularClient client;

    @InjectMocks
    private SpoonacularService spoonacularService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void searchRecipes_ShouldReturnListOfRecipes_WhenResponseIsValid() throws Exception {
        String query = "pasta";
        String cuisine = "italian";

        String jsonResponse = """
            {
                "results": [
                    {"id": 1, "title": "Spaghetti Carbonara"},
                    {"id": 2, "title": "Pasta Alfredo"}
                ]
            }
            """;

        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        when(client.search(query, cuisine)).thenReturn(new ResponseEntity<>(jsonNode, HttpStatus.OK));

        List<Recipe> recipes = spoonacularService.searchRecipes(query, cuisine);

        assertEquals(2, recipes.size());
        assertEquals(1, recipes.get(0).getId());
        assertEquals("Spaghetti Carbonara", recipes.get(0).getTitle());
        assertEquals(2, recipes.get(1).getId());
        assertEquals("Pasta Alfredo", recipes.get(1).getTitle());
    }

    @Test
    void searchRecipes_ShouldReturnEmptyList_WhenResponseBodyIsNull() {
        String query = "pasta";
        String cuisine = "italian";
        when(client.search(query, cuisine)).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<Recipe> recipes = spoonacularService.searchRecipes(query, cuisine);

        assertTrue(recipes.isEmpty());
    }

    @Test
    void searchRecipes_ShouldReturnEmptyList_WhenNoResultsInResponse() throws Exception {
        String query = "pasta";
        String cuisine = "italian";

        JsonNode jsonNode = objectMapper.readTree("{ \"results\": [] }");
        when(client.search(query, cuisine)).thenReturn(new ResponseEntity<>(jsonNode, HttpStatus.OK));

        List<Recipe> recipes = spoonacularService.searchRecipes(query, cuisine);

        assertTrue(recipes.isEmpty());
    }

    @Test
    void getRecipeInfo_ShouldReturnRecipe_WhenGivenValidId() {
        int recipeId = 123;
        Recipe mockRecipe = new Recipe();
        mockRecipe.setId(recipeId);
        mockRecipe.setTitle("Test Recipe");

        when(client.recipeInfo(recipeId)).thenReturn(new ResponseEntity<>(mockRecipe, HttpStatus.OK));

        Recipe result = spoonacularService.getRecipeInfo(recipeId);

        assertNotNull(result);
        assertEquals(recipeId, result.getId());
        assertEquals("Test Recipe", result.getTitle());
    }

    @Test
    void getCustomizedCalories_ShouldExcludeIngredientsCorrectly() {
        int recipeId = 456;

        Recipe mockRecipe = new Recipe();
        Ingredient cheese = new Ingredient("Cheese", 1, "piece", new Nutrition(40));
        Ingredient pasta = new Ingredient("Pasta", 100, "grams", new Nutrition(140));
        mockRecipe.setExtendedIngredients(List.of(cheese, pasta));

        when(client.recipeInfo(recipeId)).thenReturn(new ResponseEntity<>(mockRecipe, HttpStatus.OK));

        ExcludeRequest excludeRequest = new ExcludeRequest();
        excludeRequest.setExcludeIngredients(List.of("Cheese"));

        double totalCalories = spoonacularService.getCustomizedCalories(recipeId, excludeRequest);
        assertEquals(100, totalCalories);
    }

    @Test
    void getRecipeInfo_ShouldThrow_WhenRecipeIsMissing() {
        int invalidRecipeId = 999;
        when(client.recipeInfo(invalidRecipeId)).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        Exception exception = assertThrows(IllegalStateException.class, () -> spoonacularService.getRecipeInfo(invalidRecipeId));
        assertEquals("Failed to fetch recipe information", exception.getMessage());
    }

    @Test
    void getCustomizedCalories_ShouldIncludeAllIngredients_WhenNoneAreExcluded() {
        int recipeId = 123;
        Ingredient cheese = new Ingredient("Cheese", 1, "piece", new Nutrition(40));
        Ingredient pasta = new Ingredient("Pasta", 100, "grams", new Nutrition(140));

        Recipe mockRecipe = new Recipe();
        mockRecipe.setExtendedIngredients(List.of(cheese, pasta));
        when(client.recipeInfo(recipeId)).thenReturn(new ResponseEntity<>(mockRecipe, HttpStatus.OK));

        ExcludeRequest excludeRequest = new ExcludeRequest();
        excludeRequest.setExcludeIngredients(List.of());

        double totalCalories = spoonacularService.getCustomizedCalories(recipeId, excludeRequest);
        assertEquals(101, totalCalories);
    }
}
