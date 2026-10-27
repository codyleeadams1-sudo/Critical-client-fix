package com.criticalclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class Sounds {
	private Sounds() {}

	private static void click(float pitch) {
		try {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
		} catch (Throwable ignored) {}
	}

	private static void note(float pitch, boolean high) {
		try {
			var ev = high ? SoundEvents.NOTE_BLOCK_CHIME : SoundEvents.NOTE_BLOCK_BASS;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch));
		} catch (Throwable ignored) {}
	}

	public static void open()      { click(1.5f); }
	public static void close()     { click(0.8f); }
	public static void tab()       { click(1.1f); }
	public static void toggleOn()  { note(1.7f, true); click(1.4f); }
	public static void toggleOff() { note(0.9f, false); click(0.7f); }
}
