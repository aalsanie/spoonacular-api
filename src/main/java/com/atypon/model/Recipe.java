package com.atypon.model;

import java.util.List;

/**
 * Plain Java model (no Lombok) to keep builds stable across JDKs/environments.
 */
public class Recipe {

    private int id; // for future improvements you can use UUID
    private String title;
    private List<Ingredient> extendedIngredients;
    private Nutrition nutrition;

    public Recipe() {
    }

    public Recipe(int id, String title, List<Ingredient> extendedIngredients, Nutrition nutrition) {
        this.id = id;
        this.title = title;
        this.extendedIngredients = extendedIngredients;
        this.nutrition = nutrition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Ingredient> getExtendedIngredients() {
        return extendedIngredients;
    }

    public void setExtendedIngredients(List<Ingredient> extendedIngredients) {
        this.extendedIngredients = extendedIngredients;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
    }
}
