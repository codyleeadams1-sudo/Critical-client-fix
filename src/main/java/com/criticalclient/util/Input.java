package com.criticalclient.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Version-proof helpers. 26.x renamed/hid several Minecraft and LWJGL members,
 * so these look the right one up at runtime instead of at compile time.
 */
public final class Input {
	public static final int KEY_RIGHT_SHIFT = 344;
	public static final int KEY_C = 67;
	private static final int GLFW_PRESS = 1;

	private static Method getContext, getMouseButton, getKey;
	private static Method screenMethod;
	private static Field screenField;
	private static boolean screenResolved;

	static {
		try {
			Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
			getContext = glfw.getMethod("glfwGetCurrentContext");
			getMouseButton = glfw.getMethod("glfwGetMouseButton", long.class, int.class);
			getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
		} catch (Throwable t) {
			System.err.println("[CriticalClient] GLFW lookup failed: " + t);
		}
	}

	private Input() {}

	private static int poll(Method m, int arg) {
		try {
			long win = (long) getContext.invoke(null);
			return (int) m.invoke(null, win, arg);
		} catch (Throwable t) { return 0; }
	}

	public static boolean mouseLeftDown() { return getMouseButton != null && poll(getMouseButton, 0) == GLFW_PRESS; }
	public static boolean keyDown(int key) { return getKey != null && poll(getKey, key) == GLFW_PRESS; }

	// ---- current screen ----
	private static void resolveScreen() {
		screenResolved = true;
		for (Method m : Minecraft.class.getMethods()) {
			if (m.getParameterCount() == 0 && Screen.class.isAssignableFrom(m.getReturnType())
					&& m.getName().toLowerCase().contains("screen")) { screenMethod = m; return; }
		}
		for (Field f : Minecraft.class.getFields()) {
			if (Screen.class.isAssignableFrom(f.getType()) && f.getName().toLowerCase().contains("screen")) { screenField = f; return; }
		}
	}

	public static boolean noScreen(Minecraft mc) {
		if (!screenResolved) resolveScreen();
		try {
			if (screenMethod != null) return screenMethod.invoke(mc) == null;
			if (screenField != null) return screenField.get(mc) == null;
		} catch (Throwable ignored) {}
		return true;
	}

	// ---- key mapping creation ----
	public static KeyMapping newKey(String name, int code, KeyMapping.Category category) {
		try {
			for (Constructor<?> c : KeyMapping.class.getConstructors()) {
				Class<?>[] p = c.getParameterTypes();
				if (p.length == 3 && p[0] == String.class && p[1] == int.class && p[2] == KeyMapping.Category.class)
					return (KeyMapping) c.newInstance(name, code, category);
			}
			for (Constructor<?> c : KeyMapping.class.getConstructors()) {
				Class<?>[] p = c.getParameterTypes();
				if (p.length == 4 && p[0] == String.class && p[1].isEnum() && p[2] == int.class && p[3] == KeyMapping.Category.class) {
					Object type = p[1].getEnumConstants()[0]; // KEYSYM is the first constant
					return (KeyMapping) c.newInstance(name, type, code, category);
				}
			}
		} catch (Throwable t) { throw new RuntimeException(t); }
		throw new IllegalStateException("No usable KeyMapping constructor found");
	}
}
