package com.profitcalc;

import com.profitcalc.gui.ProfitCalculatorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ProfitCalcClient implements ClientModInitializer {
	private static KeyBinding openGuiKey;

	@Override
	public void onInitializeClient() {
		// Register keybinding
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.profit-calc.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			KeyBinding.MISC_CATEGORY
		));

		// Register tick event to check for key presses
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new ProfitCalculatorScreen(null));
				}
			}
		});
	}
}