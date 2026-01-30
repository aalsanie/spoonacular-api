package com.atypon.model;

import java.util.List;

/**
 * Plain Java model (no Lombok) to keep builds stable across JDKs/environments.
 */
public class ExcludeRequest {

    private List<String> excludeIngredients;

    public ExcludeRequest() {
    }

    public ExcludeRequest(List<String> excludeIngredients) {
        this.excludeIngredients = excludeIngredients;
    }

    public List<String> getExcludeIngredients() {
        return excludeIngredients;
    }

    public void setExcludeIngredients(List<String> excludeIngredients) {
        this.excludeIngredients = excludeIngredients;
    }
}
