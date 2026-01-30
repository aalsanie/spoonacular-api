package com.atypon.model;

/**
 * Plain Java model (no Lombok) to keep builds stable across JDKs/environments.
 */
public class Ingredient {

    private String name;
    private double amount;
    private String unit;
    private Nutrition nutrition;

    public Ingredient() {
    }

    public Ingredient(String name, double amount, String unit, Nutrition nutrition) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.nutrition = nutrition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
    }
}
