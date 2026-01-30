package com.atypon.controller;

import com.atypon.model.ExcludeRequest;
import com.atypon.model.Recipe;
import com.atypon.service.SpoonacularService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final SpoonacularService spoonacularService;

    public RecipeController(SpoonacularService spoonacularService) {
        this.spoonacularService = spoonacularService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Recipe>> searchRecipes(@RequestParam String query,
                                                      @RequestParam(required = false) String cuisine) {
        if (cuisine != null && !cuisine.isBlank()) {
            return ResponseEntity.ok(spoonacularService.searchRecipes(query, cuisine));
        }
        return ResponseEntity.ok(spoonacularService.searchRecipes(query));
    }

    @GetMapping("/recipe-info")
    public ResponseEntity<Recipe> getRecipeInfo(@RequestParam int recipeId) {
        try {
            return ResponseEntity.ok(spoonacularService.getRecipeInfo(recipeId));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/calories")
    public ResponseEntity<Double> getCustomizedCalories(@RequestParam int recipeId,
                                                        @RequestBody(required = false) ExcludeRequest excludeRequest) {
        try {
            double calories = spoonacularService.getCustomizedCalories(recipeId, excludeRequest);
            return ResponseEntity.ok(calories);
        } catch (IllegalStateException e) {
            // Tests expect 400 for invalid recipeId
            return ResponseEntity.badRequest().build();
        }
    }
}
