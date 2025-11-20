package com.profitcalc.manager;

import com.profitcalc.api.DonutSMPApiClient;
import com.profitcalc.api.model.AuctionResponse;
import com.profitcalc.model.AuctionItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionHouseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfitCalc/AuctionHouse");
    private static final AuctionHouseManager INSTANCE = new AuctionHouseManager();

    private final Map<Item, List<AuctionItem>> auctionData = new ConcurrentHashMap<>();
    private final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    private long lastApiRefresh = 0;
    private boolean isRefreshing = false;

    private AuctionHouseManager() {}

    public static AuctionHouseManager getInstance() {
        return INSTANCE;
    }

    public void addAuctionItem(ItemStack itemStack, double price, String seller) {
        AuctionItem auctionItem = new AuctionItem(itemStack, price, seller);
        Item item = itemStack.getItem();

        auctionData.computeIfAbsent(item, k -> new ArrayList<>()).add(auctionItem);

        LOGGER.info("Added auction item: {} at ${} from {}",
                   auctionItem.getDisplayName(), price, seller);
    }

    public void addAuctionItem(String itemName, double price, String seller) {
        Optional<Item> itemOpt = getItemByName(itemName);
        if (itemOpt.isPresent()) {
            ItemStack stack = new ItemStack(itemOpt.get());
            addAuctionItem(stack, price, seller);
        } else {
            LOGGER.warn("Could not find item: {}", itemName);
        }
    }

    public Optional<Double> getLowestPrice(Item item) {
        cleanOldEntries();
        List<AuctionItem> items = auctionData.get(item);
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        return items.stream()
                .mapToDouble(AuctionItem::getPrice)
                .min()
                .stream().boxed().findFirst();
    }

    public Optional<Double> getAveragePrice(Item item) {
        cleanOldEntries();
        List<AuctionItem> items = auctionData.get(item);
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        return items.stream()
                .mapToDouble(AuctionItem::getPrice)
                .average()
                .stream().boxed().findFirst();
    }

    public List<AuctionItem> getAuctionItems(Item item) {
        cleanOldEntries();
        List<AuctionItem> items = auctionData.get(item);
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void clearCache() {
        auctionData.clear();
        LOGGER.info("Cleared auction house cache");
    }

    private void cleanOldEntries() {
        long currentTime = System.currentTimeMillis();
        auctionData.values().forEach(list ->
            list.removeIf(item -> currentTime - item.getTimestamp() > CACHE_DURATION)
        );
    }

    private Optional<Item> getItemByName(String name) {
        // Try to find item by registry name
        String normalizedName = name.toLowerCase().replace(" ", "_");

        // Try with minecraft namespace first
        Identifier id = Identifier.of("minecraft", normalizedName);
        if (Registries.ITEM.containsId(id)) {
            return Optional.of(Registries.ITEM.get(id));
        }

        // Search through all items for a match
        for (Item item : Registries.ITEM) {
            String itemName = item.toString().toLowerCase();
            if (itemName.contains(normalizedName)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public Map<Item, Double> getAllLowestPrices() {
        cleanOldEntries();
        Map<Item, Double> prices = new HashMap<>();

        auctionData.forEach((item, auctionItems) -> {
            if (!auctionItems.isEmpty()) {
                double lowest = auctionItems.stream()
                        .mapToDouble(AuctionItem::getPrice)
                        .min()
                        .orElse(0);
                prices.put(item, lowest);
            }
        });

        return prices;
    }

    public CompletableFuture<Boolean> refreshFromApi() {
        if (isRefreshing) {
            LOGGER.warn("Already refreshing auction data...");
            return CompletableFuture.completedFuture(false);
        }

        isRefreshing = true;
        LOGGER.info("Starting API refresh...");

        return DonutSMPApiClient.getInstance().fetchAllAuctions()
                .thenApply(entries -> {
                    clearCache();
                    int loaded = 0;

                    for (AuctionResponse.AuctionEntry entry : entries) {
                        if (loadAuctionEntry(entry)) {
                            loaded++;
                        }
                    }

                    lastApiRefresh = System.currentTimeMillis();
                    isRefreshing = false;

                    LOGGER.info("Loaded {} auction entries from API", loaded);
                    return loaded > 0;
                })
                .exceptionally(e -> {
                    LOGGER.error("Error refreshing from API: {}", e.getMessage());
                    isRefreshing = false;
                    return false;
                });
    }

    private boolean loadAuctionEntry(AuctionResponse.AuctionEntry entry) {
        try {
            AuctionResponse.ItemData itemData = entry.getItem();
            if (itemData == null || itemData.getId() == null) {
                return false;
            }

            // Parse the item ID (e.g., "minecraft:diamond")
            Identifier itemId = Identifier.tryParse(itemData.getId());
            if (itemId == null || !Registries.ITEM.containsId(itemId)) {
                return false;
            }

            Item item = Registries.ITEM.get(itemId);
            ItemStack stack = new ItemStack(item, itemData.getCount());

            String seller = entry.getSeller() != null ? entry.getSeller().getName() : "Unknown";
            addAuctionItem(stack, entry.getPrice(), seller);

            return true;

        } catch (Exception e) {
            LOGGER.warn("Error loading auction entry: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public long getTimeSinceLastRefresh() {
        return System.currentTimeMillis() - lastApiRefresh;
    }

    public boolean needsRefresh() {
        return getTimeSinceLastRefresh() > CACHE_DURATION;
    }

    public int getTotalItems() {
        return auctionData.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
