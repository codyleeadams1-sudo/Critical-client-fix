package com.criticalclient.gui;

import com.criticalclient.module.Category;
import com.criticalclient.module.Module;
import com.criticalclient.module.ModuleManager;
import com.criticalclient.util.Anim;
import com.criticalclient.util.Colors;
import com.criticalclient.util.Gfx;
import com.criticalclient.util.Input;
import com.criticalclient.util.Sounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Critical Client module menu (Right Shift).
 * Clicks use Minecraft's real screen events. Every clickable thing registers a "hit box"
 * while it is drawn, so what you see is exactly what you can click.
 */
public class ModMenuScreen extends Screen {
	private static final long OPEN_NS = 300_000_000L, CLOSE_NS = 190_000_000L, STAGGER_NS = 30_000_000L, CARD_IN_NS = 280_000_000L;
	private static final int GAP = 8, CARD_H = 52, MIN_CARD_W = 134, R = 12;
	private static final String[] TAB_NAMES = {"All", "Visuals", "HUD", "Movement"};
	private static final Category[] TAB_CATS = {null, Category.VISUALS, Category.HUD, Category.MOVEMENT};
	private static final int WHITE = 0xFFFFFFFF;

	/** A clickable rectangle, optionally clipped (cards are clipped to the scroll area). */
	private static final class Hit {
		final int x, y, w, h, clipX, clipY, clipW, clipH;
		final Runnable action;
		Hit(int x, int y, int w, int h, int clipX, int clipY, int clipW, int clipH, Runnable action) {
			this.x = x; this.y = y; this.w = w; this.h = h;
			this.clipX = clipX; this.clipY = clipY; this.clipW = clipW; this.clipH = clipH; this.action = action;
		}
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h
				&& mx >= clipX && mx < clipX + clipW && my >= clipY && my < clipY + clipH;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	private long openedAt, closeAt, lastFrame, tabChangedAt;
	private boolean closing;
	private int tab = 0;
	private float tabY = -1, scroll, scrollTarget, closeHover, bulkHover;
	private float lastScale = 1f, lastOffY = 0f, lastP = 0f;

	private int px, py, pw, ph, sideW, cx, cy, cw, ch, cols, cardW;

	public ModMenuScreen() { super(Component.literal("Critical Client")); }

	// no @Override on purpose: keeps the game running while the menu is open
	public boolean isPauseScreen() { return false; }

	@Override
	public void onClose() {
		if (!closing) { closing = true; closeAt = System.nanoTime(); Sounds.close(); ModuleManager.save(); }
	}

	// ------------------------------------------------------------------ input
	private boolean handleClick(double sx, double sy) {
		if (closing || lastP < 0.6f) return false;
		double pcx = width / 2.0, pcy = height / 2.0;
		double lx = pcx + (sx - pcx) / lastScale;
		double ly = pcy + (sy - (pcy + lastOffY)) / lastScale;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (h.contains(lx, ly)) { h.action.run(); return true; }
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && handleClick(event.x(), event.y())) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Right Shift closes (ignore the first moment so the key that opened the menu can't close it again)
		if (event.key() == Input.KEY_RIGHT_SHIFT && openedAt != 0 && System.nanoTime() - openedAt > 450_000_000L) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	// no @Override: if the signature ever changes only wheel scrolling would stop
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget -= (float) (scrollY * 30);
		return true;
	}

	// ------------------------------------------------------------------ layout
	private List<Module> visible() {
		List<Module> out = new ArrayList<>();
		for (Module m : ModuleManager.all()) if (TAB_CATS[tab] == null || m.category == TAB_CATS[tab]) out.add(m);
		return out;
	}

	private void layout() {
		pw = Math.min(width - 12, Math.max(340, Math.min(700, (int) (width * 0.82f))));
		ph = Math.min(height - 12, Math.max(220, Math.min(380, (int) (height * 0.82f))));
		px = (width - pw) / 2; py = (height - ph) / 2;
		sideW = pw < 450 ? 96 : 124;
		cx = px + sideW + 16; cy = py + 54;
		cw = pw - sideW - 32; ch = ph - 54 - 14;
		cols = Math.max(1, (cw + GAP) / (MIN_CARD_W + GAP));
		cardW = (cw - (cols - 1) * GAP) / cols;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void hit(int x, int y, int w, int h, Runnable r) { hits.add(new Hit(x, y, w, h, x, y, w, h, r)); }

	// ------------------------------------------------------------------ render
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		long now = System.nanoTime();
		if (openedAt == 0) { openedAt = now; lastFrame = now; tabChangedAt = now; Sounds.open(); }
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		layout();
		hits.clear();

		float p = closing ? 1f - Anim.easeInCubic((now - closeAt) / (float) CLOSE_NS) : Anim.easeOutCubic((now - openedAt) / (float) OPEN_NS);
		float scale = 0.94f + 0.06f * p, offY = 16f * (1f - p);
		lastScale = scale; lastOffY = offY; lastP = p;
		float pcx = width / 2f, pcy = height / 2f;
		double mx = pcx + (mouseX - pcx) / scale;
		double my = pcy + (mouseY - (pcy + offY)) / scale;
		boolean live = !closing && p > 0.6f;

		List<Module> mods = visible();
		int rows = (mods.size() + cols - 1) / cols;
		int totalH = rows * (CARD_H + GAP) - GAP;
		int maxScroll = Math.max(0, totalH - ch);
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
		scroll = Anim.approach(scroll, scrollTarget, 18f, dt);

		Gfx g = new Gfx(graphics);
		g.setAlpha(p);
		g.gradientV(0, 0, width, height, 0x99000000, 0xDD1F060A);

		g.push();
		g.translate(pcx, pcy + offY);
		g.scale(scale);
		g.translate(-pcx, -pcy);

		for (int i = 10; i >= 1; i--) g.round(px - i, py - i + 3, pw + i * 2, ph + i * 2, R + i, Colors.mulAlpha(0xFF000000, 0.045f));
		g.round(px - 2, py - 2, pw + 4, ph + 4, R + 2, Colors.mulAlpha(Colors.ACCENT, 0.18f));
		g.round(px - 1, py - 1, pw + 2, ph + 2, R + 1, Colors.ACCENT_DIM);
		g.round(px, py, pw, ph, R, Colors.PANEL);
		g.round(px, py, sideW + R, ph, R, Colors.SIDEBAR);
		g.rect(px + sideW, py, R, ph, Colors.PANEL);
		g.rect(px + sideW, py + 12, 1, ph - 24, Colors.BORDER);
		g.gradientV(px + sideW + 1, py + R, pw - sideW - 2, 36, 0x26FF3B47, 0x00FF3B47);

		drawSidebar(g, mx, my, dt, live);
		drawHeader(g, mx, my, dt, live, mods);
		drawCards(g, mods, mx, my, dt, now, live, maxScroll);

		g.pop();

		if (closing && now - closeAt >= CLOSE_NS) super.onClose();
	}

	private int tabCount(int i, boolean onlyEnabled) {
		int n = 0;
		for (Module m : ModuleManager.all())
			if ((TAB_CATS[i] == null || m.category == TAB_CATS[i]) && (!onlyEnabled || m.isEnabled())) n++;
		return n;
	}

	/** Tiny 10x10 icons drawn from shapes. */
	private void icon(Gfx g, int idx, int x, int y, int col, int bg) {
		switch (idx) {
			case 0 -> { g.round(x, y, 4, 4, 1, col); g.round(x + 6, y, 4, 4, 1, col); g.round(x, y + 6, 4, 4, 1, col); g.round(x + 6, y + 6, 4, 4, 1, col); }
			case 1 -> { g.round(x, y + 2, 10, 6, 3, col); g.round(x + 3, y + 3, 4, 4, 2, bg); g.round(x + 4, y + 4, 2, 2, 1, col); }
			case 2 -> { g.roundBordered(x, y + 1, 10, 8, 2, bg, col); g.rect(x + 2, y + 4, 6, 1, col); g.rect(x + 2, y + 6, 4, 1, col); }
			default -> { g.rect(x, y + 4, 7, 2, col); g.rect(x + 6, y + 2, 2, 6, col); g.rect(x + 8, y + 3, 2, 4, col); }
		}
	}

	private void drawSidebar(Gfx g, double mx, double my, float dt, boolean live) {
		g.round(px + 8, py + 9, sideW - 16, 34, 8, 0xFF160D10);
		g.image("textures/gui/icon.png", px + 12, py + 15, 22, 22, 64, 64);
		int lw = sideW - 16 - 8 - 22 - 10;
		int lh = Math.round(lw * 101f / 360f);
		g.image("textures/gui/logo.png", px + 12 + 22 + 5, py + 26 - lh / 2, lw, lh, 360, 101);

		g.textScaled("MODULES", px + 12, py + 52, 0.8f, Colors.ACCENT_DIM, true);

		int tx = px + 8, tw = sideW - 16, th = 26, ty0 = py + 66;
		float targetY = ty0 + tab * (th + 4);
		tabY = tabY < 0 ? targetY : Anim.approach(tabY, targetY, 20f, dt);
		int hy = Math.round(tabY);
		g.round(tx, hy, tw, th, 7, 0xFF2B1418);
		g.round(tx, hy + 6, 3, th - 12, 1, Colors.ACCENT);
		for (int i = 0; i < TAB_NAMES.length; i++) {
			final int idx = i;
			int y = ty0 + i * (th + 4);
			boolean hov = live && in(mx, my, tx, y, tw, th);
			boolean sel = i == tab;
			if (hov && !sel) g.round(tx, y, tw, th, 7, 0xFF1A1013);
			int col = sel ? Colors.TEXT : hov ? 0xFFDCCFD2 : Colors.TEXT_DIM;
			icon(g, i, tx + 10, y + 8, sel ? Colors.ACCENT : col, sel ? 0xFF2B1418 : Colors.SIDEBAR);
			g.text(TAB_NAMES[i], tx + 26, y + 9, col, sel);
			String badge = tabCount(i, true) + "/" + tabCount(i, false);
			g.textScaled(badge, tx + tw - 6 - g.width(badge, false) * 0.8f, y + 10, 0.8f, sel ? Colors.ACCENT : 0xFF6E6064, false);
			hit(tx, y, tw, th, () -> {
				if (tab == idx) return;
				tab = idx; scrollTarget = 0; scroll = 0; tabChangedAt = System.nanoTime(); Sounds.tab();
			});
		}
		g.rect(px + 12, py + ph - 42, sideW - 24, 1, Colors.BORDER);
		g.textScaled(ModuleManager.enabledCount() + " active", px + 12, py + ph - 34, 0.85f, Colors.TEXT, true);
		g.textScaled("RSHIFT  close", px + 12, py + ph - 22, 0.8f, Colors.TEXT_DIM, false);
	}

	private void drawHeader(Gfx g, double mx, double my, float dt, boolean live, List<Module> mods) {
		String title = tab == 0 ? "All Modules" : TAB_NAMES[tab] + " Modules";
		g.text(title, cx, py + 13, Colors.TEXT, true);
		g.round(cx, py + 27, 22, 2, 1, Colors.ACCENT);
		g.textScaled(mods.size() + (mods.size() == 1 ? " module" : " modules") + "  -  click a card to toggle", cx + 28, py + 26, 0.8f, Colors.TEXT_DIM, false);

		int bs = 20, bx = px + pw - bs - 12, by = py + 11;
		boolean hov = live && in(mx, my, bx, by, bs, bs);
		closeHover = Anim.approach(closeHover, hov ? 1f : 0f, 18f, dt);
		g.round(bx, by, bs, bs, 6, Colors.lerp(0xFF1D1215, Colors.ACCENT_DIM, closeHover));
		g.textCentered("x", bx + bs / 2, by + 6, Colors.lerp(Colors.TEXT_DIM, WHITE, closeHover), true);
		hit(bx, by, bs, bs, this::onClose);

		final boolean anyOff = mods.stream().anyMatch(m -> !m.isEnabled());
		String label = anyOff ? "Enable all" : "Disable all";
		int w = g.width(label, true) + 18, h = 20, x = bx - 8 - w, y = by;
		boolean h2 = live && in(mx, my, x, y, w, h);
		bulkHover = Anim.approach(bulkHover, h2 ? 1f : 0f, 18f, dt);
		g.roundBordered(x, y, w, h, 6, Colors.lerp(0xFF1D1215, 0xFF2E1519, bulkHover), Colors.lerp(Colors.BORDER, Colors.ACCENT_DIM, bulkHover));
		g.textCentered(label, x + w / 2, y + 6, Colors.lerp(Colors.TEXT_DIM, Colors.TEXT, bulkHover), true);
		hit(x, y, w, h, () -> {
			for (Module m : mods) { m.setEnabled(anyOff); m.uiFlash = 1f; }
			if (anyOff) Sounds.toggleOn(); else Sounds.toggleOff();
			ModuleManager.save();
		});
	}

	private void drawCards(Gfx g, List<Module> mods, double mx, double my, float dt, long now, boolean live, int maxScroll) {
		float baseAlpha = g.alpha();
		g.scissor(cx - 6, cy, cw + 12, ch);
		boolean mouseInArea = in(mx, my, cx, cy, cw, ch);
		for (int i = 0; i < mods.size(); i++) {
			Module m = mods.get(i);
			int col = i % cols, row = i / cols;
			int x = cx + col * (cardW + GAP);
			int y = cy + row * (CARD_H + GAP) - Math.round(scroll);
			if (y + CARD_H + 6 < cy || y - 6 > cy + ch) continue;

			float t = Anim.easeOutCubic((now - tabChangedAt - i * STAGGER_NS) / (float) CARD_IN_NS);
			int yy = y + Math.round((1f - t) * 12f);
			g.setAlpha(baseAlpha * t);

			boolean hov = live && mouseInArea && in(mx, my, x, y, cardW, CARD_H);
			m.uiHover = Anim.approach(m.uiHover, hov ? 1f : 0f, 16f, dt);
			m.uiToggle = Anim.approach(m.uiToggle, m.isEnabled() ? 1f : 0f, 14f, dt);
			m.uiFlash = Anim.approach(m.uiFlash, 0f, 7f, dt);

			if (m.uiToggle > 0.02f) {
				g.round(x - 2, yy - 2, cardW + 4, CARD_H + 4, 10, Colors.mulAlpha(Colors.ACCENT, 0.07f * m.uiToggle));
				g.round(x - 1, yy - 1, cardW + 2, CARD_H + 2, 9, Colors.mulAlpha(Colors.ACCENT, 0.10f * m.uiToggle));
			}
			int fill = Colors.lerp(Colors.lerp(Colors.CARD, Colors.CARD_HOVER, m.uiHover), Colors.CARD_ON, m.uiToggle);
			fill = Colors.lerp(fill, 0xFF4D1B22, m.uiFlash * 0.7f);
			int border = Colors.lerp(Colors.lerp(Colors.BORDER, 0xFF55303A, m.uiHover), Colors.ACCENT_DIM, m.uiToggle);
			g.roundBordered(x, yy, cardW, CARD_H, 8, fill, border);
			g.rect(x + 8, yy + 1, cardW - 16, 1, Colors.mulAlpha(WHITE, 0.05f + 0.04f * m.uiHover));
			g.round(x + 1, yy + 12, 3, CARD_H - 24, 1, Colors.mulAlpha(Colors.ACCENT, m.uiToggle));

			int sw = 26, sh = 13, sx = x + cardW - sw - 10, sy = yy + 10;
			g.text(g.fit(m.name, cardW - sw - 34, true), x + 12, yy + 10, Colors.lerp(0xFFE6DCDE, Colors.TEXT, m.uiToggle), true);
			g.text(g.fit(m.description, cardW - 24, false), x + 12, yy + 25, Colors.TEXT_DIM, false);

			String status = m.isEnabled() ? "ENABLED" : "DISABLED";
			g.textScaled(status, x + 12, yy + 39, 0.75f, Colors.lerp(0xFF6E6064, Colors.ACCENT, m.uiToggle), true);
			String cat = m.category.label.toUpperCase();
			g.textScaled(cat, x + cardW - 10 - g.width(cat, false) * 0.75f, yy + 39, 0.75f, 0xFF6E6064, false);

			g.round(sx, sy, sw, sh, 6, Colors.lerp(Colors.TRACK_OFF, Colors.ACCENT, m.uiToggle));
			int knob = sx + 2 + Math.round((sw - sh) * m.uiToggle);
			g.round(knob, sy + 2, sh - 4, sh - 4, 4, Colors.lerp(0xFFB9ADB0, WHITE, m.uiToggle));

			final Module mod = m;
			hits.add(new Hit(x, y, cardW, CARD_H, cx - 6, cy, cw + 12, ch, () -> {
				mod.toggle();
				mod.uiFlash = 1f;
				if (mod.isEnabled()) Sounds.toggleOn(); else Sounds.toggleOff();
				ModuleManager.save();
			}));
		}
		g.setAlpha(baseAlpha);
		g.endScissor();

		if (maxScroll > 0) {
			int trackH = ch, thumbH = Math.max(18, trackH * ch / (ch + maxScroll));
			int thumbY = cy + Math.round((trackH - thumbH) * (scroll / maxScroll));
			g.round(cx + cw + 6, cy, 3, trackH, 1, 0xFF1F1417);
			g.round(cx + cw + 6, thumbY, 3, thumbH, 1, Colors.ACCENT_DIM);
		}
	}
}
