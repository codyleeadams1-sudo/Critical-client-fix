package com.criticalclient;

import com.criticalclient.gui.ModMenuScreen;
import com.criticalclient.module.ModuleManager;
import com.criticalclient.util.Gfx;
import com.criticalclient.util.Input;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;

public class CriticalClient implements ClientModInitializer {
	public static final String MOD_ID = "criticalclient";
	public static KeyMapping OPEN_KEY;
	public static KeyMapping ZOOM_KEY;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		OPEN_KEY = KeyMappingHelper.registerKeyMapping(Input.newKey("key.criticalclient.open_menu", Input.KEY_RIGHT_SHIFT, category));
		ZOOM_KEY = KeyMappingHelper.registerKeyMapping(Input.newKey("key.criticalclient.zoom", Input.KEY_C, category));

		ModuleManager.load();

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ModuleManager.tick(mc);
			while (OPEN_KEY.consumeClick()) {
				if (Input.noScreen(mc)) openScreen(mc, new ModMenuScreen());
			}
		});

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
			(graphics, tickCounter) -> {
				Minecraft mc = Minecraft.getInstance();
				ModuleManager.renderHud(new Gfx(graphics), mc,
					mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
			});
	}

	/** Minecraft.setScreen was renamed to setScreenAndShow in 26.2, so look up whichever exists. */
	public static void openScreen(Minecraft mc, Screen screen) {
		for (String name : new String[] {"setScreenAndShow", "setScreen"}) {
			try {
				Method m = Minecraft.class.getMethod(name, Screen.class);
				m.invoke(mc, screen);
				return;
			} catch (NoSuchMethodException ignored) {
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
