package com.demo16.auctionhud;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** 2D firework show drawn on the screen while the winner popup is showing. */
public final class Fireworks {
    public static final int BURSTS = 6;

    // where each burst explodes (fraction of screen) and when it launches (seconds after the win)
    private static final float[] FX = {0.20f, 0.80f, 0.50f, 0.32f, 0.68f, 0.50f};
    private static final float[] FY = {0.36f, 0.30f, 0.16f, 0.52f, 0.54f, 0.38f};
    private static final float[] LAUNCH = {0.00f, 0.15f, 0.35f, 0.60f, 0.80f, 1.10f};
    private static final float RISE = 0.35f;   // seconds the rocket flies up
    private static final float LIFE = 1.4f;    // seconds the sparks live

    private static final int[] PALETTE = {
            0xFFD21F, 0xFFFFFF, 0xFF8A00, 0xFF4D4D, 0x4DE1FF, 0x6BFF6B, 0xFF6BD6
    };

    private Fireworks() {}

    /** Seconds after the win at which burst i explodes (used for the sound). */
    public static float explodeTime(int i) {
        return LAUNCH[i] + RISE;
    }

    public static void render(DrawContext ctx, long elapsedMs, int sw, int sh) {
        float t = elapsedMs / 1000f;
        float scale = Math.max(1f, sh / 240f);

        // quick warm flash at the very start
        if (t < 0.18f) {
            int a = (int) (110 * (1f - t / 0.18f));
            ctx.fill(0, 0, sw, sh, (a << 24) | 0xFFF4C2);
        }

        for (int i = 0; i < BURSTS; i++) {
            float lt = t - LAUNCH[i];
            if (lt < 0) continue;
            int bx = (int) (sw * FX[i]);
            int by = (int) (sh * FY[i]);

            // ---- rocket flying up with a little trail ----
            if (lt < RISE) {
                float p = lt / RISE;
                p = 1f - (1f - p) * (1f - p);                     // ease out
                int ry = (int) (sh - (sh - by) * p);
                for (int k = 1; k <= 6; k++) {
                    int a = Math.max(0, 200 - k * 32);
                    ctx.fill(bx - 1, ry + k * 3, bx + 1, ry + k * 3 + 2, (a << 24) | 0xFFB347);
                }
                ctx.fill(bx - 1, ry - 1, bx + 2, ry + 2, 0xFFFFFFFF);
                continue;
            }

            // ---- explosion ----
            float et = lt - RISE;
            if (et > LIFE) continue;

            if (et < 0.09f) {                                      // bright core flash
                ctx.fill(bx - 5, by - 5, bx + 6, by + 6, 0xFFFFFFFF);
            }

            Random r = new Random(9001L * (i + 3));
            int colA = PALETTE[i % PALETTE.length];
            int colB = PALETTE[(i + 3) % PALETTE.length];
            int n = 40;
            float fade = Math.max(0f, 1f - et / LIFE);
            for (int j = 0; j < n; j++) {
                double ang = (Math.PI * 2 * j / n) + r.nextFloat() * 0.15;
                float speed = (45f + r.nextFloat() * 75f) * scale;
                // sparks slow down (drag) and then drop (gravity)
                float dist = speed * (1f - (float) Math.exp(-2.2 * et)) / 2.2f;
                float px = bx + (float) Math.cos(ang) * dist;
                float py = by + (float) Math.sin(ang) * dist + 38f * scale * et * et;

                int a = (int) (255 * fade);
                if (((j + (int) (et * 18)) % 3) == 0) a = a / 3;   // twinkle
                int rgb = (j % 2 == 0) ? colA : colB;
                int size = et < 0.35f ? 3 : 2;
                int x = (int) px, y = (int) py;
                ctx.fill(x, y, x + size, y + size, (a << 24) | rgb);
            }
        }
    }
}
