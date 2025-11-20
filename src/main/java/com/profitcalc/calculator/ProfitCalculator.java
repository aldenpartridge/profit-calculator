package com.profitcalc.calculator;

import com.profitcalc.manager.AuctionHouseManager;
import com.profitcalc.manager.RecipeManager;
import com.profitcalc.model.CraftingRecipe;
import com.profitcalc.model.ProfitCalculation;
import com.profitcalc.model.RecipeIngredient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProfitCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfitCalc/Calculator");
    private static final ProfitCalculator INSTANCE = new ProfitCalculator();

    private ProfitCalculator() {}

    public static ProfitCalculator getInstance() {
        return INSTANCE;
    }

    public List<ProfitCalculation> findProfitableItems(double maxBudget) {
        List<ProfitCalculation> profitableItems = new ArrayList<>();
        Map<Item, Double> prices = AuctionHouseManager.getInstance().getAllLowestPrices();

        if (prices.isEmpty()) {
            LOGGER.warn("No auction house data available. Please browse /ah first.");
            return profitableItems;
        }

        // Check all items that we have recipes for
        for (Item item : Registries.ITEM) {
            if (RecipeManager.getInstance().hasRecipe(item)) {
                Optional<ProfitCalculation> calculation = calculateProfit(item);

                if (calculation.isPresent()) {
                    ProfitCalculation profit = calculation.get();

                    // Check if it's profitable and within budget
                    if (profit.isProfitable() && profit.getMaterialsCost() <= maxBudget) {
                        profitableItems.add(profit);
                    }
                }
            }
        }

        // Sort by profit margin (highest first)
        profitableItems.sort((a, b) -> Double.compare(b.getProfitMargin(), a.getProfitMargin()));

        LOGGER.info("Found {} profitable items within budget ${}", profitableItems.size(), maxBudget);
        return profitableItems;
    }

    public Optional<ProfitCalculation> calculateProfit(Item item) {
        AuctionHouseManager ahManager = AuctionHouseManager.getInstance();
        RecipeManager recipeManager = RecipeManager.getInstance();

        // Get selling price for the item
        Optional<Double> sellingPriceOpt = ahManager.getLowestPrice(item);
        if (sellingPriceOpt.isEmpty()) {
            return Optional.empty();
        }

        double sellingPrice = sellingPriceOpt.get();

        // Get all available prices
        Map<Item, Double> prices = ahManager.getAllLowestPrices();

        // Get cheapest recipe
        Optional<CraftingRecipe> recipeOpt = recipeManager.getCheapestRecipe(item, prices);
        if (recipeOpt.isEmpty()) {
            return Optional.empty();
        }

        CraftingRecipe recipe = recipeOpt.get();

        // Calculate materials cost
        double materialsCost = recipeManager.calculateRecipeCost(recipe, prices);
        if (materialsCost < 0) {
            return Optional.empty(); // Missing price data
        }

        // Adjust for output quantity
        double adjustedSellingPrice = sellingPrice * recipe.getOutputQuantity();
        double adjustedMaterialsCost = materialsCost;

        // Get material prices for the calculation
        Map<Item, Double> materialPrices = new HashMap<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            Item material = ingredient.getItem();
            if (prices.containsKey(material)) {
                materialPrices.put(material, prices.get(material));
            }
        }

        ProfitCalculation calculation = new ProfitCalculation(
            item,
            adjustedSellingPrice,
            adjustedMaterialsCost,
            recipe,
            materialPrices
        );

        return Optional.of(calculation);
    }

    public List<ProfitCalculation> calculateAllProfits() {
        List<ProfitCalculation> allCalculations = new ArrayList<>();
        Map<Item, Double> prices = AuctionHouseManager.getInstance().getAllLowestPrices();

        if (prices.isEmpty()) {
            LOGGER.warn("No auction house data available");
            return allCalculations;
        }

        for (Item item : prices.keySet()) {
            if (RecipeManager.getInstance().hasRecipe(item)) {
                Optional<ProfitCalculation> calculation = calculateProfit(item);
                calculation.ifPresent(allCalculations::add);
            }
        }

        // Sort by profit (highest first)
        allCalculations.sort((a, b) -> Double.compare(b.getProfit(), a.getProfit()));

        return allCalculations;
    }

    public String formatProfit(ProfitCalculation calculation) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== ").append(calculation.getItem().getName().getString()).append(" ===\n");
        sb.append(String.format("Selling Price: $%.2f\n", calculation.getSellingPrice()));
        sb.append(String.format("Materials Cost: $%.2f\n", calculation.getMaterialsCost()));
        sb.append(String.format("Profit: $%.2f (%.1f%%)\n",
                calculation.getProfit(), calculation.getProfitMargin()));

        sb.append("\nRecipe:\n");
        CraftingRecipe recipe = calculation.getRecipe();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            Item item = ingredient.getItem();
            int quantity = ingredient.getQuantity();
            Double price = calculation.getMaterialPrices().get(item);

            sb.append(String.format("  - %dx %s @ $%.2f = $%.2f\n",
                    quantity,
                    item.getName().getString(),
                    price != null ? price : 0,
                    price != null ? price * quantity : 0));
        }

        return sb.toString();
    }
}
