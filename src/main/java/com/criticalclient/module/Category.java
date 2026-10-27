package com.criticalclient.module;

public enum Category {
	VISUALS("Visuals"), HUD("HUD"), MOVEMENT("Movement");

	public final String label;
	Category(String label) { this.label = label; }
}
