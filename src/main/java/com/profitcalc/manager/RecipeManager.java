package com.profitcalc.manager;

import com.profitcalc.model.CraftingRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfitCalc/Recipe");
    private static final RecipeManager INSTANCE = new RecipeManager();

    private final Map<Item, List<CraftingRecipe>> recipesByOutput = new ConcurrentHashMap<>();

    private RecipeManager() {}

    public static RecipeManager getInstance() {
        return INSTANCE;
    }

    public void loadRecipes(net.minecraft.recipe.RecipeManager minecraftRecipeManager) {
        recipesByOutput.clear();
        int count = 0;

        // Load crafting recipes
        for (RecipeEntry<net.minecraft.recipe.CraftingRecipe> entry :
             minecraftRecipeManager.listAllOfType(net.minecraft.recipe.RecipeType.CRAFTING)) {
            CraftingRecipe customRecipe = convertCraftingRecipe(entry.value());
            if (customRecipe != null) {
                Item output = customRecipe.getOutput();
                recipesByOutput.computeIfAbsent(output, k -> new ArrayList<>()).add(customRecipe);
                count++;
            }
        }

        // Load smelting recipes
        for (RecipeEntry<SmeltingRecipe> entry :
             minecraftRecipeManager.listAllOfType(net.minecraft.recipe.RecipeType.SMELTING)) {
            CraftingRecipe customRecipe = convertSmeltingRecipe(entry.value());
            if (customRecipe != null) {
                Item output = customRecipe.getOutput();
                recipesByOutput.computeIfAbsent(output, k -> new ArrayList<>()).add(customRecipe);
                count++;
            }
        }

        LOGGER.info("Loaded {} recipes", count);
    }

    private CraftingRecipe convertCraftingRecipe(net.minecraft.recipe.CraftingRecipe recipe) {
        try {
            // Get recipe result using the result method
            ItemStack output = recipe.getResult(null);
            if (output.isEmpty()) {
                return null;
            }

            CraftingRecipe customRecipe = new CraftingRecipe(output.getItem(), output.getCount());

            // Process ingredients
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                Map<Item, Integer> ingredientCounts = new HashMap<>();

                for (Ingredient ingredient : shapedRecipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        ItemStack[] stacks = ingredient.getMatchingStacks();
                        if (stacks.length > 0) {
                            Item item = stacks[0].getItem();
                            ingredientCounts.merge(item, 1, Integer::sum);
                        }
                    }
                }

                ingredientCounts.forEach(customRecipe::addIngredient);

            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                Map<Item, Integer> ingredientCounts = new HashMap<>();

                for (Ingredient ingredient : shapelessRecipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        ItemStack[] stacks = ingredient.getMatchingStacks();
                        if (stacks.length > 0) {
                            Item item = stacks[0].getItem();
                            ingredientCounts.merge(item, 1, Integer::sum);
                        }
                    }
                }

                ingredientCounts.forEach(customRecipe::addIngredient);
            }

            return customRecipe;

        } catch (Exception e) {
            LOGGER.warn("Failed to convert crafting recipe: {}", e.getMessage());
            return null;
        }
    }

    private CraftingRecipe convertSmeltingRecipe(SmeltingRecipe recipe) {
        try {
            // Get recipe result
            ItemStack output = recipe.getResult(null);
            if (output.isEmpty()) {
                return null;
            }

            CraftingRecipe customRecipe = new CraftingRecipe(output.getItem(), output.getCount());

            // Get ingredient
            if (!recipe.getIngredients().isEmpty()) {
                Ingredient ingredient = recipe.getIngredients().get(0);
                if (!ingredient.isEmpty()) {
                    ItemStack[] stacks = ingredient.getMatchingStacks();
                    if (stacks.length > 0) {
                        customRecipe.addIngredient(stacks[0].getItem(), 1);
                    }
                }
            }

            return customRecipe;

        } catch (Exception e) {
            LOGGER.warn("Failed to convert smelting recipe: {}", e.getMessage());
            return null;
        }
    }

    public Optional<CraftingRecipe> getCheapestRecipe(Item item, Map<Item, Double> prices) {
        List<CraftingRecipe> recipes = recipesByOutput.get(item);
        if (recipes == null || recipes.isEmpty()) {
            return Optional.empty();
        }

        CraftingRecipe cheapest = null;
        double lowestCost = Double.MAX_VALUE;

        for (CraftingRecipe recipe : recipes) {
            double cost = calculateRecipeCost(recipe, prices);
            if (cost >= 0 && cost < lowestCost) {
                lowestCost = cost;
                cheapest = recipe;
            }
        }

        return Optional.ofNullable(cheapest);
    }

    public List<CraftingRecipe> getRecipes(Item item) {
        return new ArrayList<>(recipesByOutput.getOrDefault(item, new ArrayList<>()));
    }

    public double calculateRecipeCost(CraftingRecipe recipe, Map<Item, Double> prices) {
        double totalCost = 0;

        for (var ingredient : recipe.getIngredients()) {
            Item item = ingredient.getItem();
            int quantity = ingredient.getQuantity();

            if (!prices.containsKey(item)) {
                return -1; // Can't calculate cost without price data
            }

            totalCost += prices.get(item) * quantity;
        }

        return totalCost;
    }

    public boolean hasRecipe(Item item) {
        return recipesByOutput.containsKey(item);
    }

    public void addCustomRecipe(Item output, int outputQuantity, Map<Item, Integer> ingredients) {
        CraftingRecipe recipe = new CraftingRecipe(output, outputQuantity);
        ingredients.forEach(recipe::addIngredient);

        recipesByOutput.computeIfAbsent(output, k -> new ArrayList<>()).add(recipe);
        LOGGER.info("Added custom recipe for {}", output);
    }
}
