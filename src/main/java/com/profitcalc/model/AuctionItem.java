package com.profitcalc.model;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class AuctionItem {
    private final ItemStack itemStack;
    private final double price;
    private final String sellerName;
    private final long timestamp;

    public AuctionItem(ItemStack itemStack, double price, String sellerName) {
        this.itemStack = itemStack;
        this.price = price;
        this.sellerName = sellerName;
        this.timestamp = System.currentTimeMillis();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Item getItem() {
        return itemStack.getItem();
    }

    public double getPrice() {
        return price;
    }

    public String getSellerName() {
        return sellerName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDisplayName() {
        return itemStack.getName().getString();
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f (by %s)", getDisplayName(), price, sellerName);
    }
}
