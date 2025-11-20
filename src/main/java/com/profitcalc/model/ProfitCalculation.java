package com.profitcalc.model;

import net.minecraft.item.Item;
import java.util.HashMap;
import java.util.Map;

public class ProfitCalculation {
    private final Item item;
    private final double sellingPrice;
    private final double materialsCost;
    private final double profit;
    private final double profitMargin;
    private final Map<Item, Double> materialPrices;
    private final CraftingRecipe recipe;
    private final boolean profitable;

    public ProfitCalculation(Item item, double sellingPrice, double materialsCost,
                            CraftingRecipe recipe, Map<Item, Double> materialPrices) {
        this.item = item;
        this.sellingPrice = sellingPrice;
        this.materialsCost = materialsCost;
        this.profit = sellingPrice - materialsCost;
        this.profitMargin = materialsCost > 0 ? (profit / materialsCost) * 100 : 0;
        this.recipe = recipe;
        this.materialPrices = new HashMap<>(materialPrices);
        this.profitable = profit > 0;
    }

    public Item getItem() {
        return item;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public double getMaterialsCost() {
        return materialsCost;
    }

    public double getProfit() {
        return profit;
    }

    public double getProfitMargin() {
        return profitMargin;
    }

    public Map<Item, Double> getMaterialPrices() {
        return new HashMap<>(materialPrices);
    }

    public CraftingRecipe getRecipe() {
        return recipe;
    }

    public boolean isProfitable() {
        return profitable;
    }

    @Override
    public String toString() {
        return String.format("%s: Selling=$%.2f, Cost=$%.2f, Profit=$%.2f (%.1f%%)",
                item.toString(), sellingPrice, materialsCost, profit, profitMargin);
    }
}
