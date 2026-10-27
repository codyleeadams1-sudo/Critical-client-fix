package com.criticalclient.util;

public final class Anim {
	private Anim() {}

	public static float easeOutCubic(float t) {
		t = Colors.clamp01(t);
		float u = 1f - t;
		return 1f - u * u * u;
	}

	public static float easeInCubic(float t) {
		t = Colors.clamp01(t);
		return t * t * t;
	}

	/** Frame-rate independent smoothing toward a target. */
	public static float approach(float current, float target, float rate, float dt) {
		float next = current + (target - current) * (1f - (float) Math.exp(-rate * dt));
		return Math.abs(next - target) < 0.001f ? target : next;
	}
}
