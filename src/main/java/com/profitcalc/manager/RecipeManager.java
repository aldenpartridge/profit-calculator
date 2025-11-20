package com.profitcalc.manager;

import com.profitcalc.model.CraftingRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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

    @SuppressWarnings("unchecked")
    public void loadRecipes(net.minecraft.recipe.RecipeManager minecraftRecipeManager) {
        recipesByOutput.clear();
        int count = 0;

        // Access recipes through reflection
        try {
            Field recipesField = net.minecraft.recipe.RecipeManager.class.getDeclaredField("recipes");
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<?, RecipeEntry<?>>> allRecipes =
                (Map<RecipeType<?>, Map<?, RecipeEntry<?>>>) recipesField.get(minecraftRecipeManager);

            // Load crafting recipes
            Map<?, RecipeEntry<?>> craftingRecipes = allRecipes.get(RecipeType.CRAFTING);
            if (craftingRecipes != null) {
                for (RecipeEntry<?> entry : craftingRecipes.values()) {
                    if (entry.value() instanceof net.minecraft.recipe.CraftingRecipe craftingRecipe) {
                        CraftingRecipe customRecipe = convertCraftingRecipe(craftingRecipe);
                        if (customRecipe != null) {
                            Item output = customRecipe.getOutput();
                            recipesByOutput.computeIfAbsent(output, k -> new ArrayList<>()).add(customRecipe);
                            count++;
                        }
                    }
                }
            }

            // Load smelting recipes
            Map<?, RecipeEntry<?>> smeltingRecipes = allRecipes.get(RecipeType.SMELTING);
            if (smeltingRecipes != null) {
                for (RecipeEntry<?> entry : smeltingRecipes.values()) {
                    if (entry.value() instanceof SmeltingRecipe smeltingRecipe) {
                        CraftingRecipe customRecipe = convertSmeltingRecipe(smeltingRecipe);
                        if (customRecipe != null) {
                            Item output = customRecipe.getOutput();
                            recipesByOutput.computeIfAbsent(output, k -> new ArrayList<>()).add(customRecipe);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load recipes: {}", e.getMessage(), e);
        }

        LOGGER.info("Loaded {} recipes", count);
    }

    private CraftingRecipe convertCraftingRecipe(net.minecraft.recipe.CraftingRecipe recipe) {
        try {
            // Get recipe result - use craft() method or result field
            ItemStack output = recipe.craft(null, null);
            if (output == null || output.isEmpty()) {
                return null;
            }

            CraftingRecipe customRecipe = new CraftingRecipe(output.getItem(), output.getCount());

            // Process ingredients
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                Map<Item, Integer> ingredientCounts = new HashMap<>();

                // getIngredients() returns Optional<Ingredient> in 1.21.10
                for (Optional<Ingredient> optionalIngredient : shapedRecipe.getIngredients()) {
                    if (optionalIngredient.isPresent()) {
                        Ingredient ingredient = optionalIngredient.get();
                        if (!ingredient.isEmpty()) {
                            // Get matching items - need to use getMatchingItems() or similar
                            for (ItemStack stack : ingredient.getMatchingStacks()) {
                                Item item = stack.getItem();
                                ingredientCounts.merge(item, 1, Integer::sum);
                                break; // Just use first matching item
                            }
                        }
                    }
                }

                ingredientCounts.forEach(customRecipe::addIngredient);

            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                Map<Item, Integer> ingredientCounts = new HashMap<>();

                // getInput() returns list of ingredients
                for (Ingredient ingredient : shapelessRecipe.getInput()) {
                    if (!ingredient.isEmpty()) {
                        for (ItemStack stack : ingredient.getMatchingStacks()) {
                            Item item = stack.getItem();
                            ingredientCounts.merge(item, 1, Integer::sum);
                            break; // Just use first matching item
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
            ItemStack output = recipe.craft(null, null);
            if (output == null || output.isEmpty()) {
                return null;
            }

            CraftingRecipe customRecipe = new CraftingRecipe(output.getItem(), output.getCount());

            // Get ingredient - getInput() returns the ingredient
            Ingredient ingredient = recipe.getInput();
            if (!ingredient.isEmpty()) {
                for (ItemStack stack : ingredient.getMatchingStacks()) {
                    customRecipe.addIngredient(stack.getItem(), 1);
                    break; // Just use first matching item
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
