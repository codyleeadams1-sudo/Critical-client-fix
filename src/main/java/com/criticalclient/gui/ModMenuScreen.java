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
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The Critical Client module menu. Opens with Right Shift. */
public class ModMenuScreen extends Screen {
	private static final long OPEN_NS = 280_000_000L, CLOSE_NS = 190_000_000L, STAGGER_NS = 28_000_000L, CARD_IN_NS = 260_000_000L;
	private static final int GAP = 8, CARD_H = 44, MIN_CARD_W = 124, R = 10;
	private static final String[] TAB_NAMES = {"All", "Visuals", "HUD", "Movement"};
	private static final Category[] TAB_CATS = {null, Category.VISUALS, Category.HUD, Category.MOVEMENT};

	// state
	private long openedAt, closeAt, lastFrame, tabChangedAt;
	private boolean closing, inputInit, prevMouse, prevShift;
	private int tab = 0;
	private float tabY = -1, scroll, scrollTarget, closeHover;

	// layout (recomputed each frame)
	private int px, py, pw, ph, sideW, cx, cy, cw, ch, cols, cardW;

	public ModMenuScreen() { super(Component.literal("Critical Client")); }

	// no @Override on purpose: keeps the game running while the menu is open
	public boolean isPauseScreen() { return false; }

	@Override
	public void onClose() {
		if (!closing) { closing = true; closeAt = System.nanoTime(); Sounds.close(); ModuleManager.save(); }
	}

	private List<Module> visible() {
		List<Module> out = new ArrayList<>();
		for (Module m : ModuleManager.all()) if (TAB_CATS[tab] == null || m.category == TAB_CATS[tab]) out.add(m);
		return out;
	}

	private void layout() {
		pw = Math.min(width - 12, Math.max(340, Math.min(640, (int) (width * 0.8f))));
		ph = Math.min(height - 12, Math.max(210, Math.min(340, (int) (height * 0.8f))));
		px = (width - pw) / 2; py = (height - ph) / 2;
		sideW = pw < 440 ? 90 : 112;
		cx = px + sideW + 14; cy = py + 46;
		cw = pw - sideW - 28; ch = ph - 46 - 12;
		cols = Math.max(1, (cw + GAP) / (MIN_CARD_W + GAP));
		cardW = (cw - (cols - 1) * GAP) / cols;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		long now = System.nanoTime();
		if (openedAt == 0) { openedAt = now; lastFrame = now; tabChangedAt = now; Sounds.open(); }
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		layout();

		// ---- animation progress
		float p = closing ? 1f - Anim.easeInCubic((now - closeAt) / (float) CLOSE_NS) : Anim.easeOutCubic((now - openedAt) / (float) OPEN_NS);
		float scale = 0.94f + 0.06f * p, offY = 16f * (1f - p);
		float pcx = width / 2f, pcy = height / 2f;
		double mx = pcx + (mouseX - pcx) / scale, my = pcy + (mouseY - (pcy + offY)) / scale + 0; // inverse transform
		boolean interactive = !closing && p > 0.95f;

		// ---- input (polled so it works regardless of event API changes)
		boolean down = Input.mouseLeftDown();
		boolean shift = Input.keyDown(Input.KEY_RIGHT_SHIFT);
		if (!inputInit) { prevMouse = down; prevShift = shift; inputInit = true; }
		boolean clicked = down && !prevMouse;
		if (shift && !prevShift) onClose();
		prevMouse = down; prevShift = shift;

		List<Module> mods = visible();
		int rows = (mods.size() + cols - 1) / cols;
		int totalH = rows * (CARD_H + GAP) - GAP;
		int maxScroll = Math.max(0, totalH - ch);
		scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
		scroll = Anim.approach(scroll, scrollTarget, 18f, dt);

		Gfx g = new Gfx(graphics);

		// ---- dim backdrop
		g.setAlpha(p);
		g.gradientV(0, 0, width, height, 0x99000000, 0xCC1A0508);

		// ---- panel transform
		g.push();
		g.translate(pcx, pcy + offY);
		g.scale(scale);
		g.translate(-pcx, -pcy);

		// shadow + glow border + panel
		for (int i = 8; i >= 1; i--) g.round(px - i, py - i + 2, pw + i * 2, ph + i * 2, R + i, Colors.mulAlpha(0xFF000000, 0.05f));
		g.round(px - 1, py - 1, pw + 2, ph + 2, R + 1, Colors.ACCENT_DIM);
		g.round(px, py, pw, ph, R, Colors.PANEL);
		g.round(px, py, sideW + R, ph, R, Colors.SIDEBAR);
		g.rect(px + sideW, py, R, ph, Colors.PANEL);
		g.rect(px + sideW, py + 10, 1, ph - 20, Colors.BORDER);

		drawSidebar(g, mx, my, dt, interactive, clicked);
		drawHeader(g, mx, my, dt, interactive, clicked, mods.size());
		drawCards(g, mods, mx, my, dt, now, interactive, clicked, maxScroll);

		g.pop();

		if (closing && now - closeAt >= CLOSE_NS) super.onClose(); // actually closes the screen
	}

	private void drawSidebar(Gfx g, double mx, double my, float dt, boolean interactive, boolean clicked) {
		// logo + mod icon
		g.image("textures/gui/icon.png", px + 9, py + 10, 20, 20, 64, 64);
		int lw = sideW - 9 - 20 - 6 - 8;
		g.image("textures/gui/logo.png", px + 33, py + 12, lw, Math.round(lw * 101f / 360f), 360, 101);
		g.rect(px + 10, py + 38, sideW - 20, 1, Colors.BORDER);

		// tabs
		int tx = px + 8, tw = sideW - 16, th = 22, ty0 = py + 48;
		float targetY = ty0 + tab * (th + 4);
		tabY = tabY < 0 ? targetY : Anim.approach(tabY, targetY, 20f, dt);
		g.round(tx, Math.round(tabY), tw, th, 6, 0xFF2A1317);
		g.rect(tx, Math.round(tabY) + 5, 2, th - 10, Colors.ACCENT);
		for (int i = 0; i < TAB_NAMES.length; i++) {
			int y = ty0 + i * (th + 4);
			boolean hov = interactive && in(mx, my, tx, y, tw, th);
			boolean sel = i == tab;
			int col = sel ? Colors.TEXT : hov ? 0xFFD8CCCF : Colors.TEXT_DIM;
			g.text(TAB_NAMES[i], tx + 11, y + 7, col, sel);
			if (hov && clicked && !sel) {
				tab = i; scrollTarget = 0; scroll = 0; tabChangedAt = System.nanoTime(); Sounds.tab();
			}
		}
		// footer
		g.textScaled(ModuleManager.enabledCount() + " / " + ModuleManager.all().size() + " active", px + 12, py + ph - 32, 0.85f, Colors.TEXT_DIM, false);
		g.textScaled("RSHIFT to close", px + 12, py + ph - 20, 0.85f, Colors.ACCENT_DIM, false);
	}

	private void drawHeader(Gfx g, double mx, double my, float dt, boolean interactive, boolean clicked, int count) {
		String title = tab == 0 ? "All Modules" : TAB_NAMES[tab];
		g.text(title, cx, py + 13, Colors.TEXT, true);
		g.textScaled(count + (count == 1 ? " module" : " modules"), cx, py + 27, 0.85f, Colors.TEXT_DIM, false);

		// close button
		int bx = px + pw - 28, by = py + 10, bs = 18;
		boolean hov = interactive && in(mx, my, bx, by, bs, bs);
		closeHover = Anim.approach(closeHover, hov ? 1f : 0f, 18f, dt);
		g.round(bx, by, bs, bs, 5, Colors.lerp(Colors.CARD, Colors.ACCENT_DIM, closeHover));
		g.text("x", bx + 7, by + 4, Colors.lerp(Colors.TEXT_DIM, Colors.TEXT, closeHover), true);
		if (hov && clicked) onClose();
	}

	private void drawCards(Gfx g, List<Module> mods, double mx, double my, float dt, long now, boolean interactive, boolean clicked, int maxScroll) {
		float basePanelAlpha = g.alpha();
		g.scissor(cx - 2, cy, cw + 4, ch);
		boolean mouseInArea = in(mx, my, cx, cy, cw, ch);
		for (int i = 0; i < mods.size(); i++) {
			Module m = mods.get(i);
			int col = i % cols, row = i / cols;
			int x = cx + col * (cardW + GAP);
			int y = cy + row * (CARD_H + GAP) - Math.round(scroll);
			if (y + CARD_H < cy || y > cy + ch) continue;

			// staggered entrance
			float t = Anim.easeOutCubic((now - tabChangedAt - i * STAGGER_NS) / (float) CARD_IN_NS);
			int yy = y + Math.round((1f - t) * 10f);
			g.setAlpha(basePanelAlpha * t);

			boolean hov = interactive && mouseInArea && in(mx, my, x, y, cardW, CARD_H);
			m.uiHover = Anim.approach(m.uiHover, hov ? 1f : 0f, 16f, dt);
			m.uiToggle = Anim.approach(m.uiToggle, m.isEnabled() ? 1f : 0f, 14f, dt);

			int fill = Colors.lerp(Colors.lerp(Colors.CARD, Colors.CARD_HOVER, m.uiHover), Colors.CARD_ON, m.uiToggle);
			int border = Colors.lerp(Colors.lerp(Colors.BORDER, 0xFF4A2A30, m.uiHover), Colors.ACCENT_DIM, m.uiToggle);
			g.roundBordered(x, yy, cardW, CARD_H, 7, fill, border);
			g.rect(x + 1, yy + 12, 2, Math.round(20 * m.uiToggle), Colors.ACCENT);

			int textW = cardW - 24;
			g.text(g.fit(m.name, textW - 34, true), x + 11, yy + 9, Colors.lerp(0xFFE4DADC, Colors.TEXT, m.uiToggle), true);
			g.text(g.fit(m.description, textW, false), x + 11, yy + 26, Colors.TEXT_DIM, false);

			// switch
			int sw = 24, sh = 12, sx = x + cardW - sw - 9, sy = yy + 9;
			g.round(sx, sy, sw, sh, 6, Colors.lerp(Colors.TRACK_OFF, Colors.ACCENT, m.uiToggle));
			int knob = sx + 2 + Math.round((sw - sh) * m.uiToggle);
			g.round(knob, sy + 2, sh - 4, sh - 4, 4, Colors.lerp(0xFFB9ADB0, 0xFFFFFFFF, m.uiToggle));

			if (hov && clicked) {
				m.toggle();
				if (m.isEnabled()) Sounds.toggleOn(); else Sounds.toggleOff();
				ModuleManager.save();
			}
		}
		g.setAlpha(basePanelAlpha);
		g.endScissor();

		// scrollbar
		if (maxScroll > 0) {
			int trackH = ch, thumbH = Math.max(18, trackH * ch / (ch + maxScroll));
			int thumbY = cy + Math.round((trackH - thumbH) * (scroll / maxScroll));
			g.round(cx + cw + 3, cy, 3, trackH, 1, 0xFF1F1417);
			g.round(cx + cw + 3, thumbY, 3, thumbH, 1, Colors.ACCENT_DIM);
		}
	}

	// no @Override: if the signature ever changes the menu still works, only wheel scrolling would stop
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget -= (float) (scrollY * 30);
		return true;
	}
}
