package com.criticalclient.util;

/** Red-themed palette + colour math. All colours are ARGB ints. */
public final class Colors {
	public static final int PANEL      = 0xFF120C0E;
	public static final int SIDEBAR    = 0xFF0C0709;
	public static final int CARD       = 0xFF1A1114;
	public static final int CARD_HOVER = 0xFF241619;
	public static final int CARD_ON    = 0xFF2B1216;
	public static final int BORDER     = 0xFF2C1D21;
	public static final int ACCENT     = 0xFFFF3B47;
	public static final int ACCENT_DIM = 0xFF9E2430;
	public static final int TEXT       = 0xFFF4EEEF;
	public static final int TEXT_DIM   = 0xFF9C8F93;
	public static final int TRACK_OFF  = 0xFF3A2D31;
	public static final int HUD_BG     = 0xB0100B0D;

	private Colors() {}

	public static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

	public static int mulAlpha(int c, float m) {
		int a = Math.round((c >>> 24) * clamp01(m));
		return (a << 24) | (c & 0xFFFFFF);
	}

	public static int lerp(int c1, int c2, float t) {
		t = clamp01(t);
		int a = (int) ((c1 >>> 24) + ((c2 >>> 24) - (c1 >>> 24)) * t);
		int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
		int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
		int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
