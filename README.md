# Minecraft Profit Calculator Mod

A Fabric mod for Minecraft 1.21.10 that helps you calculate profit margins for crafting items by comparing auction house prices with material costs.

## Features

- **Automatic Auction House Price Tracking**: Monitors chat messages to extract item prices from the auction house
- **Recipe Database**: Automatically loads all crafting and smelting recipes from Minecraft
- **Profit Calculation**: Calculates the profit margin for crafting items based on current auction prices
- **Budget Filter**: Find profitable items within your budget
- **GUI Interface**: Easy-to-use interface with detailed profit breakdowns
- **Keybinding**: Press `P` to open the profit calculator (configurable in controls)

## How to Use

### 1. Install the Mod
1. Make sure you have Fabric Loader installed for Minecraft 1.21.10
2. Download Fabric API 0.138.3+1.21.10 or later
3. Build this mod using `./gradlew build`
4. Place the generated JAR file from `build/libs/` into your `.minecraft/mods` folder

### 2. Collect Price Data
Before using the calculator, you need to collect auction house price data:
1. Join the server (DonutSMP)
2. Browse the auction house using `/ah`
3. The mod will automatically parse and cache item prices from chat messages
4. The price data is cached for 5 minutes

### 3. Calculate Profits
1. Press `P` to open the Profit Calculator GUI
2. Enter your budget (how much money you want to spend on materials)
3. Click "Calculate Profits"
4. View the list of profitable items sorted by profit margin
5. Click on any item to see detailed cost breakdown

## How It Works

### Auction House Parser
The mod monitors chat messages and looks for auction house listings in common formats:
- `ItemName - $price - Seller`
- `[AH] ItemName: $price (Seller)`
- `ItemName for $price by Seller`

If your server uses a different format, you may need to modify the regex patterns in `ChatMessageMixin.java`.

### Recipe Loading
Recipes are automatically loaded from Minecraft's recipe manager when you join a world. This includes:
- Crafting table recipes (shaped and shapeless)
- Smelting recipes

### Profit Calculation
For each item:
1. Find the lowest auction house price (selling price)
2. Find the cheapest crafting recipe
3. Calculate materials cost using lowest prices from auction house
4. Calculate profit: `Selling Price - Materials Cost`
5. Calculate profit margin: `(Profit / Materials Cost) * 100%`

## Building

Requirements:
- Java 21 or higher
- Gradle 9.1.0 (included via wrapper)

```bash
./gradlew build
```

The compiled mod will be in `build/libs/profit-calc-1.0.0.jar`

## Configuration

### Keybinding
The default keybinding is `P`. You can change this in Minecraft's Controls settings under the "Profit Calculator" category.

### Custom Recipes
You can add custom recipes programmatically using the `RecipeManager`:

```java
RecipeManager.getInstance().addCustomRecipe(
    outputItem,
    outputQuantity,
    Map.of(
        ingredientItem1, quantity1,
        ingredientItem2, quantity2
    )
);
```

### Cache Duration
Price data is cached for 5 minutes by default. You can modify this in `AuctionHouseManager.java`:

```java
private final long CACHE_DURATION = 5 * 60 * 1000; // milliseconds
```

## Project Structure

```
src/
├── main/
│   ├── java/com/profitcalc/
│   │   ├── ProfitCalc.java              # Main mod initializer
│   │   ├── calculator/
│   │   │   └── ProfitCalculator.java    # Core profit calculation logic
│   │   ├── manager/
│   │   │   ├── AuctionHouseManager.java # Auction data storage
│   │   │   └── RecipeManager.java       # Recipe database
│   │   └── model/
│   │       ├── AuctionItem.java         # Auction item data
│   │       ├── CraftingRecipe.java      # Recipe data
│   │       ├── RecipeIngredient.java    # Recipe ingredient
│   │       └── ProfitCalculation.java   # Profit calculation result
│   └── resources/
│       ├── fabric.mod.json              # Mod metadata
│       └── assets/profit-calc/
│           ├── icon.png
│           └── lang/en_us.json          # English translations
└── client/
    ├── java/com/profitcalc/
    │   ├── ProfitCalcClient.java        # Client initializer
    │   ├── gui/
    │   │   └── ProfitCalculatorScreen.java # Main GUI screen
    │   └── mixin/client/
    │       ├── ChatMessageMixin.java    # Chat message parser
    │       └── ClientPlayNetworkHandlerMixin.java # Recipe loader
    └── resources/
        └── profit-calc.client.mixins.json
```

## Dependencies

- Minecraft 1.21.10
- Fabric Loader 0.17.3 or higher
- Fabric API 0.138.3+1.21.10 or higher
- Java 21 or higher

## License

This mod is released under CC0-1.0 (Public Domain).

## Troubleshooting

### No items showing in the calculator
- Make sure you've browsed the auction house first to collect price data
- Check that your server's auction house format matches the patterns in the code
- Verify that recipes are loaded (they load when you join a world)

### Prices seem incorrect
- Prices are cached for 5 minutes, try refreshing auction house data
- The mod uses the lowest available price for each item
- Make sure the chat message format is correctly parsed

### Custom auction house format
If your server uses a different format, edit the regex patterns in `src/client/java/com/profitcalc/mixin/client/ChatMessageMixin.java`:

```java
private static final Pattern PATTERN_1 = Pattern.compile("your-pattern-here");
```

## Future Enhancements

Potential features for future versions:
- Export profit calculations to file
- Historical price tracking
- Price alerts
- Profit trends over time
- Multi-world support
- Command-line interface
- Custom profit calculation strategies
