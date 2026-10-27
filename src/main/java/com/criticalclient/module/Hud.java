package com.criticalclient.module;

import com.criticalclient.util.Colors;
import com.criticalclient.util.Gfx;

/** Shared HUD helpers: the stacked info pills in the top-left corner. */
public final class Hud {
	public static int leftY = 4;
	private Hud() {}

	public static void pill(Gfx g, String label, String value, int valueColor) {
		int lw = g.width(label, false);
		int vw = g.width(value, true);
		int w = lw + vw + 16;
		g.round(4, leftY, w, 14, 4, Colors.HUD_BG);
		g.rect(4, leftY + 3, 2, 8, Colors.ACCENT);
		g.text(label, 10, leftY + 3, Colors.TEXT_DIM, false);
		g.text(value, 10 + lw + 4, leftY + 3, valueColor, true);
		leftY += 17;
	}
}
