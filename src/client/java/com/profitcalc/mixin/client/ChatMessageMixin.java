package com.profitcalc.mixin.client;

import com.profitcalc.manager.AuctionHouseManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatMessageMixin {
    // Common auction house message patterns
    // Pattern 1: "ItemName - $price - Seller"
    private static final Pattern PATTERN_1 = Pattern.compile("(.+?)\\s*-\\s*\\$([0-9,.]+)\\s*-\\s*(.+)");
    // Pattern 2: "[AH] ItemName: $price (Seller)"
    private static final Pattern PATTERN_2 = Pattern.compile("\\[AH\\]\\s*(.+?):\\s*\\$([0-9,.]+)\\s*\\((.+?)\\)");
    // Pattern 3: "ItemName for $price by Seller"
    private static final Pattern PATTERN_3 = Pattern.compile("(.+?)\\s+for\\s+\\$([0-9,.]+)\\s+by\\s+(.+)");

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onChatMessage(Text message, CallbackInfo ci) {
        String text = message.getString();

        // Try to parse auction house messages
        parseAuctionMessage(text);
    }

    private void parseAuctionMessage(String message) {
        // Try each pattern
        Matcher matcher1 = PATTERN_1.matcher(message);
        if (matcher1.find()) {
            addParsedAuction(matcher1.group(1), matcher1.group(2), matcher1.group(3));
            return;
        }

        Matcher matcher2 = PATTERN_2.matcher(message);
        if (matcher2.find()) {
            addParsedAuction(matcher2.group(1), matcher2.group(2), matcher2.group(3));
            return;
        }

        Matcher matcher3 = PATTERN_3.matcher(message);
        if (matcher3.find()) {
            addParsedAuction(matcher3.group(1), matcher3.group(2), matcher3.group(3));
        }
    }

    private void addParsedAuction(String itemName, String priceStr, String seller) {
        try {
            // Remove commas and parse price
            double price = Double.parseDouble(priceStr.replace(",", ""));

            // Clean up item name (remove color codes, etc.)
            String cleanItemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
            String cleanSeller = seller.replaceAll("§[0-9a-fk-or]", "").trim();

            AuctionHouseManager.getInstance().addAuctionItem(cleanItemName, price, cleanSeller);
        } catch (NumberFormatException e) {
            // Failed to parse price, ignore
        }
    }
}
