package com.atypon.controller;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestTemplate;

import com.atypon.model.ExcludeRequest;
import com.atypon.model.Ingredient;
import com.atypon.model.Nutrition;
import com.atypon.model.Recipe;

import java.util.List;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecipeControllerStressTest {

    @MockBean
    private RestTemplate restTemplate; // Mocked to simulate API calls

    @Autowired
    private WebTestClient webTestClient;

    @RepeatedTest(100)  // Repeat the test 100 times to simulate stress
    void stressTestForGetCustomizedCalories() {
        Recipe recipe = new Recipe();
        Ingredient cheese = new Ingredient("Cheese", 1, "piece", new Nutrition(40));
        Ingredient pasta = new Ingredient("Pasta", 100, "grams", new Nutrition(140));
        recipe.setExtendedIngredients(List.of(cheese, pasta));

        // Mock the RestTemplate to return the predefined recipe
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), isNull(), eq(Recipe.class)))
                .thenReturn(new ResponseEntity<>(recipe, HttpStatus.OK));

        // Create the exclude request
        ExcludeRequest excludeRequest = new ExcludeRequest();
        excludeRequest.setExcludeIngredients(List.of("Cheese"));

        // Act: Make the POST request and verify the result
        webTestClient.post()
                .uri("/api/recipes/calories?recipeId=123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(excludeRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(100.0);  // Expected calories excluding Cheese
    }

}
