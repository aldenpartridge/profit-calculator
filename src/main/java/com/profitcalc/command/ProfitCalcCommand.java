package com.profitcalc.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.profitcalc.api.DonutSMPApiClient;
import com.profitcalc.config.ConfigManager;
import com.profitcalc.manager.AuctionHouseManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ProfitCalcCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("profitcalc")
                .then(CommandManager.literal("apikey")
                        .then(CommandManager.argument("key", StringArgumentType.string())
                                .executes(ProfitCalcCommand::setApiKey)))
                .then(CommandManager.literal("refresh")
                        .executes(ProfitCalcCommand::refreshAuctions))
                .then(CommandManager.literal("status")
                        .executes(ProfitCalcCommand::showStatus))
                .executes(ProfitCalcCommand::showHelp));
    }

    private static int setApiKey(CommandContext<ServerCommandSource> context) {
        String apiKey = StringArgumentType.getString(context, "key");

        context.getSource().sendFeedback(() ->
                Text.literal("§eValidating API key..."), false);

        DonutSMPApiClient.getInstance().testApiKey(apiKey).thenAccept(valid -> {
            if (valid) {
                ConfigManager.getInstance().setApiKey(apiKey);
                context.getSource().sendFeedback(() ->
                        Text.literal("§aAPI key set successfully!"), false);
                context.getSource().sendFeedback(() ->
                        Text.literal("§aUse /profitcalc refresh to load auction data"), false);
            } else {
                context.getSource().sendFeedback(() ->
                        Text.literal("§cInvalid API key! Please check and try again."), false);
                context.getSource().sendFeedback(() ->
                        Text.literal("§eGenerate a key on DonutSMP with /api"), false);
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int refreshAuctions(CommandContext<ServerCommandSource> context) {
        String apiKey = ConfigManager.getInstance().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            context.getSource().sendFeedback(() ->
                    Text.literal("§cNo API key set! Use /profitcalc apikey <key>"), false);
            return 0;
        }

        if (AuctionHouseManager.getInstance().isRefreshing()) {
            context.getSource().sendFeedback(() ->
                    Text.literal("§eAlready refreshing auction data..."), false);
            return 0;
        }

        context.getSource().sendFeedback(() ->
                Text.literal("§eRefreshing auction data from DonutSMP API..."), false);

        AuctionHouseManager.getInstance().refreshFromApi().thenAccept(success -> {
            if (success) {
                int count = AuctionHouseManager.getInstance().getTotalItems();
                context.getSource().sendFeedback(() ->
                        Text.literal(String.format("§aLoaded %d auction entries!", count)), false);
            } else {
                context.getSource().sendFeedback(() ->
                        Text.literal("§cFailed to load auction data. Check logs for details."), false);
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        String apiKey = ConfigManager.getInstance().getApiKey();
        int totalItems = AuctionHouseManager.getInstance().getTotalItems();
        long timeSinceRefresh = AuctionHouseManager.getInstance().getTimeSinceLastRefresh();
        boolean isRefreshing = AuctionHouseManager.getInstance().isRefreshing();

        context.getSource().sendFeedback(() ->
                Text.literal("§6=== Profit Calculator Status ==="), false);

        context.getSource().sendFeedback(() ->
                Text.literal(String.format("§eAPI Key: %s",
                        (apiKey != null && !apiKey.isEmpty()) ? "§aSet" : "§cNot set")), false);

        context.getSource().sendFeedback(() ->
                Text.literal(String.format("§eCached Items: §f%d", totalItems)), false);

        if (timeSinceRefresh > 0) {
            long minutes = timeSinceRefresh / 60000;
            context.getSource().sendFeedback(() ->
                    Text.literal(String.format("§eLast Refresh: §f%d minutes ago", minutes)), false);
        }

        context.getSource().sendFeedback(() ->
                Text.literal(String.format("§eRefreshing: %s",
                        isRefreshing ? "§aYes" : "§7No")), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() ->
                Text.literal("§6=== Profit Calculator Commands ==="), false);
        context.getSource().sendFeedback(() ->
                Text.literal("§e/profitcalc apikey <key> §7- Set your DonutSMP API key"), false);
        context.getSource().sendFeedback(() ->
                Text.literal("§e/profitcalc refresh §7- Refresh auction data from API"), false);
        context.getSource().sendFeedback(() ->
                Text.literal("§e/profitcalc status §7- Show current status"), false);
        context.getSource().sendFeedback(() ->
                Text.literal("§7Generate an API key on DonutSMP with §e/api"), false);

        return Command.SINGLE_SUCCESS;
    }
}
