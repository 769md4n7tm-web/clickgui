package com.example.clickgui;

import net.minecraft.util.math.MathHelper;

public final class Rainbow {
    public static boolean enabled = true;
    public static float speed = 1f;
    private static final int FALLBACK = 0xFF2F6BFF;

    /** Animated rainbow color. offset shifts the hue so nearby pixels form a wave. */
    public static int color(float offset) {
        if (!enabled) return FALLBACK;
        float h = ((System.currentTimeMillis() % 6000L) / 6000f * speed + offset) % 1f;
        if (h < 0) h += 1f;
        // MathHelper instead of java.awt.Color: AWT is missing on many mobile JREs.
        return 0xFF000000 | MathHelper.hsvToRgb(h, 0.7f, 1f);
    }

    public static int alpha(int argb, int a) {
        return (argb & 0x00FFFFFF) | (a << 24);
    }
}
