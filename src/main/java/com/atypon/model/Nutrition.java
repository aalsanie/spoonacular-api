package com.atypon.model;

/**
 * Plain Java model (no Lombok) to keep builds stable across JDKs/environments.
 */
public class Nutrition {

    private double calories;

    public Nutrition() {
    }

    public Nutrition(double calories) {
        this.calories = calories;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }
}
