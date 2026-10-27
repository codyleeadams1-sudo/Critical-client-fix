package com.criticalclient.module;

import com.criticalclient.CriticalClient;
import com.criticalclient.util.Colors;
import com.criticalclient.util.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** All built-in modules. */
public final class Modules {
	private Modules() {}

	// ------------------------------------------------------------ VISUALS
	public static final class Fullbright extends Module {
		private boolean owned;
		public Fullbright() { super("fullbright", "Fullbright", "Maximum brightness everywhere", Category.VISUALS, false); }

		@Override public void onTick(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p == null) { owned = false; return; }
			MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
			if (cur == null) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
				owned = true;
			}
		}

		@Override protected void onDisable() {
			Minecraft mc = Minecraft.getInstance();
			if (owned && mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
			owned = false;
		}
	}

	public static final class Zoom extends Module {
		private Integer original;
		public Zoom() { super("zoom", "Zoom", "Hold C to zoom in", Category.VISUALS, false); }

		@Override public void onTick(Minecraft mc) {
			var fov = mc.options.fov();
			boolean want = CriticalClient.ZOOM_KEY != null && CriticalClient.ZOOM_KEY.isDown() && mc.screen == null;
			if (want && original == null) original = fov.get();
			if (original == null) return;
			int target = want ? 30 : original;
			int cur = fov.get();
			if (cur != target) {
				int next = Math.round(cur + (target - cur) * 0.5f);
				if (next == cur) next = target;
				fov.set(next);
			}
			if (!want && fov.get().intValue() == original.intValue()) original = null;
		}

		@Override protected void onDisable() {
			Minecraft mc = Minecraft.getInstance();
			if (original != null) { mc.options.fov().set(original); original = null; }
		}
	}

	// ------------------------------------------------------------ MOVEMENT
	public static final class AutoSprint extends Module {
		private boolean forced;
		public AutoSprint() { super("autosprint", "Auto Sprint", "Sprint while moving forward", Category.MOVEMENT, false); }

		@Override public void onTick(Minecraft mc) {
			if (mc.player == null) { forced = false; return; }
			boolean forward = mc.options.keyUp.isDown() && mc.screen == null;
			if (forward) { mc.options.keySprint.setDown(true); forced = true; }
			else if (forced) { mc.options.keySprint.setDown(false); forced = false; }
		}

		@Override protected void onDisable() {
			if (forced) { Minecraft.getInstance().options.keySprint.setDown(false); forced = false; }
		}
	}

	// ------------------------------------------------------------ HUD
	public static final class Coordinates extends Module {
		private static final String[] DIRS = {"S", "W", "N", "E"};
		public Coordinates() { super("coords", "Coordinates", "Position and facing", Category.HUD, true); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			LocalPlayer p = mc.player; if (p == null) return;
			int dir = Mth.floor(p.getYRot() / 90f + 0.5f) & 3;
			String v = Mth.floor(p.getX()) + "  " + Mth.floor(p.getY()) + "  " + Mth.floor(p.getZ()) + "  " + DIRS[dir];
			Hud.pill(g, "XYZ", v, Colors.TEXT);
		}
	}

	public static final class FpsCounter extends Module {
		private int frames, shown; private long last = System.nanoTime();
		public FpsCounter() { super("fps", "FPS Counter", "Live frame rate", Category.HUD, true); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			frames++;
			long now = System.nanoTime();
			if (now - last >= 500_000_000L) { shown = Math.round(frames * 1e9f / (now - last)); frames = 0; last = now; }
			int col = shown >= 60 ? Colors.TEXT : shown >= 30 ? 0xFFFFC857 : Colors.ACCENT;
			Hud.pill(g, "FPS", Integer.toString(shown), col);
		}
	}

	public static final class Clock extends Module {
		private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
		public Clock() { super("clock", "Clock", "Real-world time", Category.HUD, false); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			Hud.pill(g, "TIME", LocalTime.now().format(FMT), Colors.TEXT);
		}
	}

	public static final class TotemCounter extends Module {
		public TotemCounter() { super("totems", "Totem Counter", "Totems in your inventory", Category.HUD, false); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			LocalPlayer p = mc.player; if (p == null) return;
			var inv = p.getInventory();
			int n = 0;
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount();
			}
			Hud.pill(g, "TOTEMS", Integer.toString(n), n > 0 ? Colors.TEXT : Colors.ACCENT);
		}
	}

	public static final class NumberedHotbar extends Module {
		public NumberedHotbar() { super("hotbarnumbers", "Numbered Hotbar", "Slot numbers 1-9", Category.HUD, true); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			LocalPlayer p = mc.player; if (p == null) return;
			int selected = p.getInventory().getSelectedSlot();
			for (int i = 0; i < 9; i++) {
				int x = sw / 2 - 91 + i * 20 + 4;
				int y = sh - 22 + 2;
				boolean sel = i == selected;
				g.textScaled(Integer.toString(i + 1), x, y, 0.75f, sel ? Colors.ACCENT : 0xCCFFFFFF, true);
			}
		}
	}

	/** Shared durability row used by the armour + held item modules. */
	static void durabilityRow(Gfx g, ItemStack s, int right, int y) {
		if (s.isEmpty() || !s.isDamageableItem()) return;
		int max = s.getMaxDamage();
		int left = max - s.getDamageValue();
		float ratio = Mth.clamp(left / (float) max, 0f, 1f);
		int col = ratio > 0.5f ? Colors.lerp(0xFFFFC857, 0xFF5BE37D, (ratio - 0.5f) * 2f) : Colors.lerp(Colors.ACCENT, 0xFFFFC857, ratio * 2f);
		String t = Integer.toString(left);
		int w = 26 + g.width(t, true) + 6;
		int x = right - w;
		g.round(x, y, w, 18, 5, Colors.HUD_BG);
		g.item(s, x + 3, y + 1);
		g.text(t, x + 22, y + 3, Colors.TEXT, true);
		g.rect(x + 5, y + 15, w - 10, 2, 0x33FFFFFF);
		g.rect(x + 5, y + 15, Math.max(1, Math.round((w - 10) * ratio)), 2, col);
	}

	public static final class ArmorDurability extends Module {
		private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		public ArmorDurability() { super("armor", "Armor Durability", "Armor wear on your HUD", Category.HUD, true); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			LocalPlayer p = mc.player; if (p == null) return;
			int y = sh / 2 - 40;
			for (EquipmentSlot slot : SLOTS) {
				ItemStack s = p.getItemBySlot(slot);
				if (s.isEmpty() || !s.isDamageableItem()) continue;
				durabilityRow(g, s, sw - 4, y);
				y += 20;
			}
		}
	}

	public static final class HeldDurability extends Module {
		public HeldDurability() { super("helddurability", "Tool Durability", "Durability of held item", Category.HUD, false); }

		@Override public void renderHud(Gfx g, Minecraft mc, int sw, int sh) {
			LocalPlayer p = mc.player; if (p == null) return;
			durabilityRow(g, p.getMainHandItem(), sw - 4, sh / 2 + 46);
		}
	}
}
