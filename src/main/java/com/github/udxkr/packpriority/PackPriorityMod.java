package com.github.udxkr.packpriority;

import com.github.udxkr.packpriority.config.PackPriorityConfigManager;
import com.github.udxkr.packpriority.core.PackPriorityEngine;
import com.github.udxkr.packpriority.screen.PackPriorityScreen;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PackPriorityMod implements ClientModInitializer {

	public static final String MOD_ID = "resourcepack-priorizer";
	public static final Logger LOGGER = LoggerFactory.getLogger("ResourcePack Priorizer");

	private static KeyBinding openScreenKey;

	@Override
	public void onInitializeClient() {
		PackPriorityConfigManager.init(LOGGER);

		openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.resourcepack-priorizer.open",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_P,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openScreenKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new PackPriorityScreen(null));
				}
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof PackScreen)) {
				return;
			}
			Screens.getButtons(screen).add(ButtonWidget.builder(
							Text.translatable("screen.resourcepack-priorizer.button"),
							button -> client.setScreen(new PackPriorityScreen(screen)))
					.dimensions(width / 2 - 154, height - 48, 100, 20)
					.build());
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PackPriorityEngine.resetSessionState());

		LOGGER.info("ResourcePack Priorizer ready (local packs can outrank server packs)");
	}

	public static boolean applyAndReload(MinecraftClient client) {
		ResourcePackManager manager = client.getResourcePackManager();

		List<String> before = new ArrayList<>(manager.getEnabledIds());
		List<String> baseline = PackPriorityEngine.baselineIds();
		manager.setEnabledProfiles(baseline.isEmpty() ? before : baseline);
		List<String> after = new ArrayList<>(manager.getEnabledIds());

		if (before.equals(after)) {

			return false;
		}
		client.reloadResources();
		return true;
	}
}
