package com.criticalclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Saves which modules are on to config/criticalclient.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("criticalclient.json");

	private Config() {}

	public static Map<String, Boolean> load() {
		try {
			if (Files.exists(FILE)) {
				Map<String, Boolean> m = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<String, Boolean>>() {}.getType());
				if (m != null) return m;
			}
		} catch (Exception e) {
			System.err.println("[CriticalClient] Could not read config: " + e);
		}
		return new LinkedHashMap<>();
	}

	public static void save(Map<String, Boolean> state) {
		try {
			Files.writeString(FILE, GSON.toJson(state));
		} catch (Exception e) {
			System.err.println("[CriticalClient] Could not save config: " + e);
		}
	}
}
