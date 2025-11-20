package com.profitcalc;

import com.profitcalc.command.ProfitCalcCommand;
import com.profitcalc.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfitCalc implements ModInitializer {
	public static final String MOD_ID = "profit-calc";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Profit Calculator mod initialized!");

		// Load configuration
		ConfigManager.getInstance().load();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				ProfitCalcCommand.register(dispatcher));

		LOGGER.info("Press P to open the profit calculator GUI");

		String apiKey = ConfigManager.getInstance().getApiKey();
		if (apiKey == null || apiKey.isEmpty()) {
			LOGGER.warn("No API key configured. Use /profitcalc apikey <key> to set it.");
			LOGGER.warn("Generate an API key on DonutSMP with /api");
		}
	}
}