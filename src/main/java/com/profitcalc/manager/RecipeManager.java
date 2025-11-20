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
                            // Use reflection to get matching stacks
                            ItemStack[] stacks = getMatchingStacksReflection(ingredient);
                            if (stacks != null && stacks.length > 0) {
                                Item item = stacks[0].getItem();
                                ingredientCounts.merge(item, 1, Integer::sum);
                            }
                        }
                    }
                }

                ingredientCounts.forEach(customRecipe::addIngredient);

            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                Map<Item, Integer> ingredientCounts = new HashMap<>();

                // Use reflection to get ingredients
                List<Ingredient> ingredients = getIngredientsReflection(shapelessRecipe);
                for (Ingredient ingredient : ingredients) {
                    if (!ingredient.isEmpty()) {
                        ItemStack[] stacks = getMatchingStacksReflection(ingredient);
                        if (stacks != null && stacks.length > 0) {
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
            ItemStack output = recipe.craft(null, null);
            if (output == null || output.isEmpty()) {
                return null;
            }

            CraftingRecipe customRecipe = new CraftingRecipe(output.getItem(), output.getCount());

            // Get ingredient using reflection
            Ingredient ingredient = getIngredientReflection(recipe);
            if (ingredient != null && !ingredient.isEmpty()) {
                ItemStack[] stacks = getMatchingStacksReflection(ingredient);
                if (stacks != null && stacks.length > 0) {
                    customRecipe.addIngredient(stacks[0].getItem(), 1);
                }
            }

            return customRecipe;

        } catch (Exception e) {
            LOGGER.warn("Failed to convert smelting recipe: {}", e.getMessage());
            return null;
        }
    }

    // Helper method to get matching stacks using reflection
    private ItemStack[] getMatchingStacksReflection(Ingredient ingredient) {
        try {
            // Try getMatchingStacks() first
            try {
                var method = Ingredient.class.getMethod("getMatchingStacks");
                return (ItemStack[]) method.invoke(ingredient);
            } catch (NoSuchMethodException e) {
                // Try accessing the field directly
                Field matchingStacksField = Ingredient.class.getDeclaredField("matchingStacks");
                matchingStacksField.setAccessible(true);
                return (ItemStack[]) matchingStacksField.get(ingredient);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get matching stacks: {}", e.getMessage());
            return new ItemStack[0];
        }
    }

    // Helper method to get ingredients from ShapelessRecipe using reflection
    @SuppressWarnings("unchecked")
    private List<Ingredient> getIngredientsReflection(ShapelessRecipe recipe) {
        try {
            // Try getInput() first
            try {
                var method = ShapelessRecipe.class.getMethod("getInput");
                return (List<Ingredient>) method.invoke(recipe);
            } catch (NoSuchMethodException e) {
                // Try getIngredients()
                try {
                    var method = ShapelessRecipe.class.getMethod("getIngredients");
                    return (List<Ingredient>) method.invoke(recipe);
                } catch (NoSuchMethodException e2) {
                    // Try accessing field directly
                    Field inputField = ShapelessRecipe.class.getDeclaredField("input");
                    inputField.setAccessible(true);
                    return (List<Ingredient>) inputField.get(recipe);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get ingredients from shapeless recipe: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper method to get ingredient from SmeltingRecipe using reflection
    private Ingredient getIngredientReflection(SmeltingRecipe recipe) {
        try {
            // Try getInput() first
            try {
                var method = SmeltingRecipe.class.getMethod("getInput");
                return (Ingredient) method.invoke(recipe);
            } catch (NoSuchMethodException e) {
                // Try getIngredients()
                try {
                    var method = SmeltingRecipe.class.getMethod("getIngredients");
                    List<?> ingredients = (List<?>) method.invoke(recipe);
                    if (!ingredients.isEmpty()) {
                        return (Ingredient) ingredients.get(0);
                    }
                } catch (NoSuchMethodException e2) {
                    // Try accessing field directly
                    Field inputField = SmeltingRecipe.class.getDeclaredField("input");
                    inputField.setAccessible(true);
                    return (Ingredient) inputField.get(recipe);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get ingredient from smelting recipe: {}", e.getMessage());
        }
        return null;
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
