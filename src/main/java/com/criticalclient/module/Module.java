package com.criticalclient.module;

import com.criticalclient.util.Gfx;
import net.minecraft.client.Minecraft;

public abstract class Module {
	public final String id, name, description;
	public final Category category;
	private boolean enabled;

	// GUI animation state
	public float uiHover, uiToggle;

	protected Module(String id, String name, String description, Category category, boolean defaultOn) {
		this.id = id; this.name = name; this.description = description; this.category = category;
		this.enabled = defaultOn;
		this.uiToggle = defaultOn ? 1f : 0f;
	}

	public boolean isEnabled() { return enabled; }

	public void setEnabled(boolean on) {
		if (on == enabled) return;
		enabled = on;
		try { if (on) onEnable(); else onDisable(); } catch (Throwable t) { t.printStackTrace(); }
	}

	/** Used when loading config: set the flag without running callbacks. */
	public void setEnabledSilently(boolean on) { enabled = on; uiToggle = on ? 1f : 0f; }

	public void toggle() { setEnabled(!enabled); }

	protected void onEnable() {}
	protected void onDisable() {}
	public void onTick(Minecraft mc) {}
	public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {}
}
