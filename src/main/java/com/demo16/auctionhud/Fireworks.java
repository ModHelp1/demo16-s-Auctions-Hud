package com.demo16.auctionhud;

import java.util.Random;
import net.minecraft.class_332;

/**
 * Clean 2D firework show + falling confetti, drawn behind the winner screen.
 * Everything is deterministic (same show every time) and only uses plain rectangles.
 */
public final class Fireworks {
    public static final int BURSTS = 8;

    // where each burst explodes (fraction of the screen) and when its rocket launches (seconds after the win)
    private static final float[] FX = {0.17f, 0.83f, 0.35f, 0.65f, 0.08f, 0.92f, 0.25f, 0.75f};
    private static final float[] FY = {0.30f, 0.27f, 0.15f, 0.17f, 0.54f, 0.50f, 0.42f, 0.40f};
    private static final float[] LAUNCH = {0.00f, 0.25f, 0.55f, 0.95f, 1.40f, 1.80f, 2.35f, 2.95f};
    private static final float RISE = 0.55f;   // seconds the rocket flies up
    private static final float LIFE = 1.7f;    // seconds the sparks live
    private static final float DRAG = 2.6f;

    private static final int[] COLORS = {
            0xFFD84D, 0xFF6BB5, 0x5FE0FF, 0x7CFF8A, 0xFF9A3C, 0xB48CFF, 0xFFFFFF, 0xFF5A5A
    };

    // ---- confetti (generated once) ----
    private static final int CONF = 90;
    private static final float[] C_X = new float[CONF], C_DELAY = new float[CONF], C_FALL = new float[CONF],
            C_SWAY = new float[CONF], C_FREQ = new float[CONF], C_PH = new float[CONF], C_SPIN = new float[CONF],
            C_W = new float[CONF], C_H = new float[CONF];
    private static final int[] C_COL = new int[CONF];

    static {
        Random r = new Random(77L);
        for (int i = 0; i < CONF; i++) {
            C_X[i] = 0.03f + r.nextFloat() * 0.94f;
            C_DELAY[i] = r.nextFloat() * 2.2f;
            C_FALL[i] = 55f + r.nextFloat() * 45f;
            C_SWAY[i] = 6f + r.nextFloat() * 14f;
            C_FREQ[i] = 1.5f + r.nextFloat() * 2.5f;
            C_PH[i] = r.nextFloat() * 6.2832f;
            C_SPIN[i] = 3f + r.nextFloat() * 6f;
            C_W[i] = 3f + r.nextFloat() * 2.5f;
            C_H[i] = 1.5f + r.nextFloat() * 1.5f;
            C_COL[i] = r.nextInt(COLORS.length);
        }
    }

    private Fireworks() {}

    /** Seconds after the win at which burst i explodes (used for the sound). */
    public static float explodeTime(int i) {
        return LAUNCH[i] + RISE;
    }

    public static void render(class_332 ctx, long elapsedMs, int sw, int sh, int accent, float master) {
        float t = elapsedMs / 1000f;
        float scale = Math.max(1f, sh / 240f);

        // soft warm flash at the very start
        if (t < 0.22f) {
            ctx.method_25294(0, 0, sw, sh, Gfx.alpha(0xFFFFF4C2, (1f - t / 0.22f) * 0.28f * master));
        }

        confetti(ctx, t, sw, sh, scale, accent, master);

        for (int i = 0; i < BURSTS; i++) {
            float lt = t - LAUNCH[i];
            if (lt < 0) continue;
            int bx = (int) (sw * FX[i]);
            int by = (int) (sh * FY[i]);

            if (lt < RISE) {
                rocket(ctx, lt, bx, by, sh, master);
                continue;
            }
            float et = lt - RISE;
            if (et > LIFE) continue;
            burst(ctx, i, et, bx, by, scale, accent, master);
        }
    }

    // ---------------------------------------------------------------------
    private static float rocketY(float lt, int by, int sh) {
        float p = Gfx.clamp01(lt / RISE);
        p = 1f - (1f - p) * (1f - p) * (1f - p);          // ease out
        return sh - (sh - by) * p;
    }

    private static void rocket(class_332 ctx, float lt, int bx, int by, int sh, float master) {
        for (int k = 9; k >= 1; k--) {
            float tt = lt - k * 0.022f;
            if (tt < 0) continue;
            int ry = (int) rocketY(tt, by, sh);
            int c = Gfx.alpha(0xFFFFC875, (1f - k / 10f) * 0.8f * master);
            ctx.method_25294(bx, ry, bx + 1, ry + 2, c);
        }
        int hy = (int) rocketY(lt, by, sh);
        ctx.method_25294(bx - 1, hy - 1, bx + 2, hy + 2, Gfx.alpha(0xFFFFFFFF, master));
    }

    private static void burst(class_332 ctx, int i, float et, int bx, int by, float scale, int accent, float master) {
        Random r = new Random(9001L * (i + 3));
        int colA = (i == 0 || i == 5) ? (accent & 0xFFFFFF) : COLORS[i % COLORS.length];
        int colB = COLORS[(i + 3) % COLORS.length];

        // bright core flash
        if (et < 0.16f) {
            float f = 1f - et / 0.16f;
            Gfx.disc(ctx, bx, by, (int) (9 * scale * (0.5f + 0.5f * (1f - f))), Gfx.alpha(0xFFFFFFFF, 0.35f * f * master));
            Gfx.sparkle(ctx, bx, by, (int) (7 * scale), 0xFFFFFFFF, f * master);
        }

        float fade = 1f - Gfx.clamp01(et / LIFE);
        fade = fade * fade * (3f - 2f * fade);
        fade *= master;
        float hot = Gfx.clamp01(1f - et / 0.25f);           // white-hot at the start

        // outer ring
        int n = 40;
        float base = 165f * scale;
        for (int j = 0; j < n; j++) {
            double ang = Math.PI * 2 * j / n + (r.nextFloat() - 0.5f) * 0.05f;
            float speed = base * (0.96f + r.nextFloat() * 0.08f);
            spark(ctx, bx, by, (float) Math.cos(ang), (float) Math.sin(ang), speed, 26f * scale, et,
                    (j % 2 == 0) ? colA : colB, fade, hot, 2, 4);
        }
        // inner ring (smaller, other colour, rotated half a step)
        int n2 = 22;
        for (int j = 0; j < n2; j++) {
            double ang = Math.PI * 2 * (j + 0.5) / n2;
            float speed = base * 0.52f * (0.97f + r.nextFloat() * 0.06f);
            spark(ctx, bx, by, (float) Math.cos(ang), (float) Math.sin(ang), speed, 20f * scale, et,
                    colB, fade, hot, 2, 3);
        }
        // glitter that hangs around and drifts down
        for (int j = 0; j < 14; j++) {
            double ang = r.nextFloat() * Math.PI * 2;
            float speed = base * (0.3f + r.nextFloat() * 0.7f);
            float tw = 0.55f + 0.45f * (float) Math.sin(et * 38f + j * 1.7f);
            spark(ctx, bx, by, (float) Math.cos(ang), (float) Math.sin(ang), speed, 55f * scale, et,
                    0xFFF0C0, fade * tw, 0f, 1, 0);
        }
    }

    /** One spark with a short fading tail. */
    private static void spark(class_332 ctx, int bx, int by, float cos, float sin, float speed, float grav, float et,
                              int rgb, float fade, float hot, int head, int tail) {
        for (int k = 0; k <= tail; k++) {
            float tt = et - k * 0.03f;
            if (tt < 0) break;
            float d = speed * (1f - (float) Math.exp(-DRAG * tt)) / DRAG;
            int px = (int) (bx + cos * d);
            int py = (int) (by + sin * d + grav * tt * tt);
            float a = fade * (1f - k * 0.24f);
            if (a <= 0.02f) continue;
            int size = (k == 0) ? head : 1;
            int c = Gfx.blend(0xFF000000 | rgb, 0xFFFFFFFF, k == 0 ? hot : 0f);
            ctx.method_25294(px, py, px + size, py + size, Gfx.alpha(c, a));
        }
    }

    // ---------------------------------------------------------------------
    private static void confetti(class_332 ctx, float t, int sw, int sh, float scale, int accent, float master) {
        var m = ctx.method_51448();
        float unit = Math.max(1f, scale * 0.8f);
        for (int i = 0; i < CONF; i++) {
            float lt = t - C_DELAY[i];
            if (lt < 0) continue;
            float y = -8f + lt * C_FALL[i] * scale;
            if (y > sh + 8) continue;
            float x = C_X[i] * sw + (float) Math.sin(lt * C_FREQ[i] + C_PH[i]) * C_SWAY[i] * scale;
            float flip = Math.abs((float) Math.cos(lt * C_SPIN[i] + C_PH[i]));
            float w = Math.max(0.6f, C_W[i] * unit * flip);
            float h = C_H[i] * unit;
            int rgb = (i % 6 == 0) ? (accent & 0xFFFFFF) : COLORS[C_COL[i]];
            int c = Gfx.alpha(0xFF000000 | rgb, master);
            m.pushMatrix();
            m.translate(x, y);
            m.rotate(lt * C_SPIN[i] * 0.5f + C_PH[i]);
            m.scale(w / 2f, h / 2f);
            ctx.method_25294(-1, -1, 1, 1, c);
            m.popMatrix();
        }
    }
}
