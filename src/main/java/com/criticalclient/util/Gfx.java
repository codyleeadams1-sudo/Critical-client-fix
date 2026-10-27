package com.criticalclient.util;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * One thin wrapper around GuiGraphicsExtractor. If Mojang renames a drawing
 * call in a future version, this is the only file that needs touching.
 * Text uses bundled TTF fonts (smooth, not pixelated).
 */
public final class Gfx {
	private static final Map<String, Component> FONT_CACHE = new HashMap<>();
	private static Method ITEM_METHOD;
	static {
		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			Class<?>[] p = m.getParameterTypes();
			if (m.getParameterCount() == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class
					&& (m.getName().equals("item") || m.getName().equals("renderItem"))) {
				ITEM_METHOD = m;
				break;
			}
		}
	}

	private final GuiGraphicsExtractor g;
	private final Minecraft mc = Minecraft.getInstance();
	private float alpha = 1f;

	public Gfx(GuiGraphicsExtractor g) { this.g = g; }

	public GuiGraphicsExtractor raw() { return g; }
	public void setAlpha(float a) { this.alpha = Colors.clamp01(a); }
	public float alpha() { return alpha; }
	private int c(int color) { return alpha >= 1f ? color : Colors.mulAlpha(color, alpha); }

	// ---------- shapes ----------
	public void rect(int x, int y, int w, int h, int color) {
		if (w <= 0 || h <= 0) return;
		int col = c(color);
		if ((col >>> 24) == 0) return;
		g.fill(x, y, x + w, y + h, col);
	}

	public void gradientV(int x, int y, int w, int h, int top, int bottom) {
		g.fillGradient(x, y, x + w, y + h, c(top), c(bottom));
	}

	/** Filled rounded rectangle (scanline based). */
	public void round(int x, int y, int w, int h, int r, int color) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) { rect(x, y, w, h, color); return; }
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = (int) Math.round(r - Math.sqrt((double) r * r - dy * dy));
			rect(x + inset, y + i, w - inset * 2, 1, color);
			rect(x + inset, y + h - 1 - i, w - inset * 2, 1, color);
		}
		rect(x, y + r, w, h - r * 2, color);
	}

	/** Rounded rect with a 1px border (both colours should be opaque). */
	public void roundBordered(int x, int y, int w, int h, int r, int fill, int border) {
		round(x, y, w, h, r, border);
		round(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
	}

	// ---------- text ----------
	private static Component styled(String s, boolean bold) {
		String key = (bold ? "b|" : "r|") + s;
		Component comp = FONT_CACHE.get(key);
		if (comp == null) {
			try {
				JsonObject o = new JsonObject();
				o.addProperty("text", s);
				o.addProperty("font", "criticalclient:" + (bold ? "ui_bold" : "ui"));
				comp = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, o).result().orElseGet(() -> Component.literal(s));
			} catch (Throwable t) {
				comp = Component.literal(s);
			}
			if (FONT_CACHE.size() > 1024) FONT_CACHE.clear();
			FONT_CACHE.put(key, comp);
		}
		return comp;
	}

	public int width(String s, boolean bold) { return mc.font.width(styled(s, bold)); }

	public void text(String s, int x, int y, int color, boolean bold) {
		g.text(mc.font, styled(s, bold), x, y, c(color), false);
	}

	public void textRight(String s, int right, int y, int color, boolean bold) {
		text(s, right - width(s, bold), y, color, bold);
	}

	public void textCentered(String s, int cx, int y, int color, boolean bold) {
		text(s, cx - width(s, bold) / 2, y, color, bold);
	}

	public void textScaled(String s, float x, float y, float scale, int color, boolean bold) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		text(s, 0, 0, color, bold);
		g.pose().popMatrix();
	}

	public String fit(String s, int maxW, boolean bold) {
		if (width(s, bold) <= maxW) return s;
		String t = s;
		while (t.length() > 1 && width(t + "...", bold) > maxW) t = t.substring(0, t.length() - 1);
		return t + "...";
	}

	// ---------- images / items ----------
	public void image(String path, int x, int y, int w, int h, int texW, int texH) {
		Identifier id = Identifier.fromNamespaceAndPath("criticalclient", path);
		g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
	}

	public void item(ItemStack stack, int x, int y) {
		if (ITEM_METHOD == null || stack.isEmpty()) return;
		try { ITEM_METHOD.invoke(g, stack, x, y); } catch (Throwable ignored) {}
	}

	// ---------- transforms / clipping ----------
	public void push() { g.pose().pushMatrix(); }
	public void pop() { g.pose().popMatrix(); }
	public void translate(float x, float y) { g.pose().translate(x, y); }
	public void scale(float s) { g.pose().scale(s, s); }
	public void scissor(int x, int y, int w, int h) { g.enableScissor(x, y, x + w, y + h); }
	public void endScissor() { g.disableScissor(); }
}
