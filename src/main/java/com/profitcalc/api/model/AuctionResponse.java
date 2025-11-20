package com.profitcalc.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AuctionResponse {
    @SerializedName("result")
    private List<AuctionEntry> result;

    @SerializedName("status")
    private int status;

    public List<AuctionEntry> getResult() {
        return result;
    }

    public int getStatus() {
        return status;
    }

    public static class AuctionEntry {
        @SerializedName("item")
        private ItemData item;

        @SerializedName("price")
        private double price;

        @SerializedName("seller")
        private SellerData seller;

        @SerializedName("time_left")
        private long timeLeft;

        public ItemData getItem() {
            return item;
        }

        public double getPrice() {
            return price;
        }

        public SellerData getSeller() {
            return seller;
        }

        public long getTimeLeft() {
            return timeLeft;
        }
    }

    public static class ItemData {
        @SerializedName("id")
        private String id;

        @SerializedName("count")
        private int count;

        @SerializedName("display_name")
        private String displayName;

        @SerializedName("lore")
        private List<String> lore;

        @SerializedName("enchants")
        private EnchantData enchants;

        public String getId() {
            return id;
        }

        public int getCount() {
            return count;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }

        public EnchantData getEnchants() {
            return enchants;
        }
    }

    public static class EnchantData {
        @SerializedName("enchantments")
        private EnchantmentList enchantments;

        @SerializedName("trim")
        private TrimData trim;

        public EnchantmentList getEnchantments() {
            return enchantments;
        }

        public TrimData getTrim() {
            return trim;
        }
    }

    public static class EnchantmentList {
        @SerializedName("levels")
        private java.util.Map<String, Integer> levels;

        public java.util.Map<String, Integer> getLevels() {
            return levels;
        }
    }

    public static class TrimData {
        @SerializedName("material")
        private String material;

        @SerializedName("pattern")
        private String pattern;

        public String getMaterial() {
            return material;
        }

        public String getPattern() {
            return pattern;
        }
    }

    public static class SellerData {
        @SerializedName("name")
        private String name;

        @SerializedName("uuid")
        private String uuid;

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }
    }
}
