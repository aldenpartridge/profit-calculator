package com.profitcalc.model;

import net.minecraft.item.Item;
import java.util.ArrayList;
import java.util.List;

public class CraftingRecipe {
    private final Item output;
    private final int outputQuantity;
    private final List<RecipeIngredient> ingredients;

    public CraftingRecipe(Item output, int outputQuantity) {
        this.output = output;
        this.outputQuantity = outputQuantity;
        this.ingredients = new ArrayList<>();
    }

    public void addIngredient(Item item, int quantity) {
        ingredients.add(new RecipeIngredient(item, quantity));
    }

    public Item getOutput() {
        return output;
    }

    public int getOutputQuantity() {
        return outputQuantity;
    }

    public List<RecipeIngredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(outputQuantity).append("x ").append(output.toString()).append(" = ");
        for (int i = 0; i < ingredients.size(); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(ingredients.get(i).toString());
        }
        return sb.toString();
    }
}
