package com.criticalclient.module;

import com.criticalclient.util.Config;
import com.criticalclient.util.Gfx;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final List<Module> MODULES = List.of(
		new Modules.Fullbright(),
		new Modules.Zoom(),
		new Modules.ArmorDurability(),
		new Modules.HeldDurability(),
		new Modules.NumberedHotbar(),
		new Modules.Coordinates(),
		new Modules.FpsCounter(),
		new Modules.Clock(),
		new Modules.TotemCounter(),
		new Modules.AutoSprint()
	);

	private ModuleManager() {}

	public static List<Module> all() { return MODULES; }

	public static void load() {
		Map<String, Boolean> saved = Config.load();
		for (Module m : MODULES) {
			Boolean v = saved.get(m.id);
			if (v != null) m.setEnabledSilently(v);
		}
	}

	public static void save() {
		Map<String, Boolean> state = new LinkedHashMap<>();
		for (Module m : MODULES) state.put(m.id, m.isEnabled());
		Config.save(state);
	}

	public static int enabledCount() {
		int n = 0;
		for (Module m : MODULES) if (m.isEnabled()) n++;
		return n;
	}

	public static void tick(Minecraft mc) {
		for (Module m : MODULES) if (m.isEnabled()) {
			try { m.onTick(mc); } catch (Throwable t) { t.printStackTrace(); }
		}
	}

	public static void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
		if (mc.player == null) return;
		Hud.leftY = 4;
		for (Module m : MODULES) if (m.isEnabled()) {
			try { m.renderHud(g, mc, sw, sh); } catch (Throwable t) { t.printStackTrace(); }
		}
	}
}
