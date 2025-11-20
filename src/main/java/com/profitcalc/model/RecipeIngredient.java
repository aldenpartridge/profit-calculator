package com.profitcalc.model;

import net.minecraft.item.Item;

public class RecipeIngredient {
    private final Item item;
    private final int quantity;

    public RecipeIngredient(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return quantity + "x " + item.toString();
    }
}
