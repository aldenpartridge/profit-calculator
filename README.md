# Minecraft Profit Calculator Mod

A Fabric mod for Minecraft 1.21.10 that helps you calculate profit margins for crafting items by comparing auction house prices with material costs on DonutSMP.

## Features

- **DonutSMP API Integration**: Fetches real-time auction house data using the official DonutSMP Public API
- **Recipe Database**: Automatically loads all crafting and smelting recipes from Minecraft
- **Profit Calculation**: Calculates the profit margin for crafting items based on current auction prices
- **Budget Filter**: Find profitable items within your budget
- **GUI Interface**: Easy-to-use interface with detailed profit breakdowns
- **Keybinding**: Press `P` to open the profit calculator (configurable in controls)
- **In-Game Commands**: Manage API key and refresh data with simple commands

## How to Use

### 1. Install the Mod
1. Make sure you have Fabric Loader installed for Minecraft 1.21.10
2. Download Fabric API 0.138.3+1.21.10 or later
3. Build this mod using `./gradlew build` (requires internet connection)
4. Place the generated JAR file from `build/libs/` into your `.minecraft/mods` folder

### 2. Setup API Key
The mod uses the DonutSMP Public API to get auction house data:

1. Join DonutSMP server
2. Run `/api` to generate an API key
3. Copy the API key
4. Set it using one of these methods:
   - **In-Game Command**: `/profitcalc apikey <your-key>`
   - **GUI**: Press `P`, paste the key in the "DonutSMP API Key" field, click "Save Key"

### 3. Load Auction Data
1. Press `P` to open the Profit Calculator GUI
2. Click "Refresh Auction Data" button
3. Wait for data to load (typically 5-10 seconds)
4. You'll see a status message showing how many items were loaded

### 4. Calculate Profits
1. In the Profit Calculator GUI, enter your budget
2. Click "Calculate Profits"
3. View the list of profitable items sorted by profit margin
4. Click on any item to see detailed cost breakdown with material prices

## Commands

- `/profitcalc apikey <key>` - Set your DonutSMP API key
- `/profitcalc refresh` - Manually refresh auction data from API
- `/profitcalc status` - Show current status (cached items, last refresh, etc.)
- `/profitcalc` - Show help message

## How It Works

### DonutSMP API Integration
The mod fetches auction house data directly from the DonutSMP Public API:
- **Endpoint**: `https://api.donutsmp.net/v1/auction/list/{page}`
- **Authentication**: Bearer token (your API key)
- **Rate Limit**: 250 requests per minute
- **Data**: Real-time auction listings with item IDs, prices, sellers, and time remaining

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
- Internet connection (for Gradle dependencies and API access)

```bash
./gradlew build
```

The compiled mod will be in `build/libs/profit-calc-1.0.0.jar`

## Configuration

### Config File
The mod creates a configuration file at `config/profit-calc.json`:

```json
{
  "apiKey": "your-api-key-here",
  "autoRefresh": true,
  "refreshIntervalMinutes": 5
}
```

- **apiKey**: Your DonutSMP API key
- **autoRefresh**: Currently not implemented (future feature)
- **refreshIntervalMinutes**: Cache duration (5 minutes default)

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

## Project Structure

```
src/
├── main/
│   ├── java/com/profitcalc/
│   │   ├── ProfitCalc.java              # Main mod initializer
│   │   ├── api/
│   │   │   ├── DonutSMPApiClient.java   # API client for DonutSMP
│   │   │   └── model/
│   │   │       └── AuctionResponse.java # API response models
│   │   ├── calculator/
│   │   │   └── ProfitCalculator.java    # Core profit calculation logic
│   │   ├── command/
│   │   │   └── ProfitCalcCommand.java   # In-game commands
│   │   ├── config/
│   │   │   └── ConfigManager.java       # Configuration management
│   │   ├── manager/
│   │   │   ├── AuctionHouseManager.java # Auction data storage & API integration
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
    │   │   └── ProfitCalculatorScreen.java # Main GUI with API key management
    │   └── mixin/client/
    │       ├── ChatMessageMixin.java    # Chat message parser (legacy/backup)
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

## API Information

### DonutSMP Public API
- **Base URL**: `https://api.donutsmp.net/`
- **Version**: 1.0
- **Authentication**: Bearer token (generate with `/api` in-game)
- **Rate Limit**: 250 requests per minute per API key
- **Endpoints Used**:
  - `GET /v1/auction/list/{page}` - List all current auction entries
  - Supports search and sort parameters (not currently used by the mod)

### API Response Structure
```json
{
  "result": [
    {
      "item": {
        "id": "minecraft:diamond",
        "count": 1,
        "display_name": "Diamond"
      },
      "price": 100.0,
      "seller": {
        "name": "PlayerName",
        "uuid": "player-uuid"
      },
      "time_left": 3600000
    }
  ],
  "status": 200
}
```

## Troubleshooting

### No items showing in the calculator
- Make sure you've set your API key first
- Click "Refresh Auction Data" to load prices from the API
- Check that recipes are loaded (they load when you join a world)
- Use `/profitcalc status` to check if data is loaded

### "Unauthorized" or "Invalid API key" errors
- Generate a new API key on DonutSMP with `/api`
- Make sure you copied the entire key
- Re-save the key using `/profitcalc apikey <key>` or through the GUI

### Refresh is slow or times out
- The API has a rate limit of 250 requests/minute
- Large auction houses may take 10-30 seconds to load
- Check your internet connection
- View logs for detailed error messages

### Prices seem incorrect
- Prices are cached for 5 minutes
- Click "Refresh Auction Data" to get latest prices
- The mod uses the lowest available price for each item

### Build fails with network errors
- The Gradle build requires internet to download dependencies
- Check your firewall settings
- Try building with a VPN if blocked

## Future Enhancements

Potential features for future versions:
- **Auto-refresh**: Automatically refresh auction data periodically
- **Transaction History**: Use `/v1/auction/transactions` for price trends
- **Export**: Export profit calculations to CSV/JSON
- **Historical Analysis**: Track price trends over time
- **Price Alerts**: Notify when profitable items are found
- **Search & Filter**: Search for specific items in the GUI
- **Advanced Sorting**: Sort by profit amount, margin, or ROI
- **Multi-crafting**: Calculate bulk crafting profits
- **Batch Crafting**: Show how many times you can craft within budget
