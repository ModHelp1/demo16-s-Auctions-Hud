package com.demo16.auctionhud;

import net.minecraft.class_327;
import net.minecraft.class_332;

/** Small drawing helpers shared by the HUD, the style picker and the winner screen. */
final class Gfx {
    private Gfx() {}

    static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    /** Multiplies the alpha of an ARGB colour by f (0..1). */
    static int alpha(int argb, float f) {
        int a = (int) (((argb >>> 24) & 0xFF) * clamp01(f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    static int blend(int c1, int c2, float t) {
        t = clamp01(t);
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int opaque(int argb) { return 0xFF000000 | (argb & 0xFFFFFF); }

    static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    static void border(class_332 ctx, int x, int y, int w, int h, int color) {
        ctx.method_25294(x, y, x + w, y + 1, color);
        ctx.method_25294(x, y + h - 1, x + w, y + h, color);
        ctx.method_25294(x, y, x + 1, y + h, color);
        ctx.method_25294(x + w - 1, y, x + w, y + h, color);
    }

    /** Text that is skipped when nearly invisible (Minecraft draws alpha < 4 as fully opaque). */
    static void text(class_332 ctx, class_327 font, String s, int x, int y, int color, boolean shadow) {
        if (((color >>> 24) & 0xFF) < 8) return;
        ctx.method_51433(font, s, x, y, color, shadow);
    }

    static void centered(class_332 ctx, class_327 font, String s, int cx, int y, int color, boolean shadow) {
        text(ctx, font, s, cx - font.method_1727(s) / 2, y, color, shadow);
    }

    /** Text at a float position and scale (top-left at x,y). */
    static void scaled(class_332 ctx, class_327 font, String s, float x, float y, float scale, int color, boolean shadow) {
        if (((color >>> 24) & 0xFF) < 8) return;
        var m = ctx.method_51448();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        ctx.method_51433(font, s, 0, 0, color, shadow);
        m.popMatrix();
    }

    static void centeredScaled(class_332 ctx, class_327 font, String s, float cx, float y, float scale, int color, boolean shadow) {
        scaled(ctx, font, s, cx - font.method_1727(s) * scale / 2f, y, scale, color, shadow);
    }

    /** Width in pixels of text drawn with spaced(). */
    static float spacedWidth(class_327 font, String s, float scale, float spacing) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += font.method_1727(String.valueOf(s.charAt(i))) + spacing;
        return (w - spacing) * scale;
    }

    /** Letter-spaced text, centred on cx. spacing is in text pixels (before scale). */
    static void spaced(class_332 ctx, class_327 font, String s, float cx, float y, float scale, float spacing, int color, boolean shadow) {
        if (((color >>> 24) & 0xFF) < 8) return;
        var m = ctx.method_51448();
        m.pushMatrix();
        m.translate(cx - spacedWidth(font, s, scale, spacing) / 2f, y);
        m.scale(scale, scale);
        float cur = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            ctx.method_51433(font, ch, (int) cur, 0, color, shadow);
            cur += font.method_1727(ch) + spacing;
        }
        m.popMatrix();
    }

    /** Filled circle made of scanlines. */
    static void disc(class_332 ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt((double) r * r - (double) dy * dy);
            ctx.method_25294(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    /** 4-point twinkle. */
    static void sparkle(class_332 ctx, int cx, int cy, int len, int color, float intensity) {
        if (intensity <= 0.02f || len < 1) return;
        for (int i = 1; i <= len; i++) {
            int c = alpha(color, (1f - (float) i / (len + 1)) * intensity);
            ctx.method_25294(cx + i, cy, cx + i + 1, cy + 1, c);
            ctx.method_25294(cx - i, cy, cx - i + 1, cy + 1, c);
            ctx.method_25294(cx, cy + i, cx + 1, cy + i + 1, c);
            ctx.method_25294(cx, cy - i, cx + 1, cy - i + 1, c);
        }
        int d = Math.max(1, len / 3);
        int dc = alpha(color, 0.45f * intensity);
        ctx.method_25294(cx + d, cy + d, cx + d + 1, cy + d + 1, dc);
        ctx.method_25294(cx - d, cy + d, cx - d + 1, cy + d + 1, dc);
        ctx.method_25294(cx + d, cy - d, cx + d + 1, cy - d + 1, dc);
        ctx.method_25294(cx - d, cy - d, cx - d + 1, cy - d + 1, dc);
        ctx.method_25294(cx - 1, cy - 1, cx + 2, cy + 2, alpha(color, 0.55f * intensity));
        ctx.method_25294(cx, cy, cx + 1, cy + 1, alpha(0xFFFFFFFF, intensity));
    }

    private static final String[] STAR = {
            "...#...",
            "...#...",
            "#######",
            ".#####.",
            "..###..",
            ".##.##.",
            ".#...#."
    };

    /** Pixel-art star centred on cx,cy. */
    static void star(class_332 ctx, int cx, int cy, int unit, int color) {
        int x0 = cx - (7 * unit) / 2, y0 = cy - (7 * unit) / 2;
        int dark = alpha(blend(color, 0xFF000000, 0.65f), 0.9f);
        for (int pass = 0; pass < 2; pass++) {
            int off = pass == 0 ? Math.max(1, unit / 2) : 0;
            int c = pass == 0 ? dark : color;
            for (int r = 0; r < 7; r++) {
                for (int q = 0; q < 7; q++) {
                    if (STAR[r].charAt(q) == '#') {
                        int px = x0 + q * unit + off, py = y0 + r * unit + off;
                        ctx.method_25294(px, py, px + unit, py + unit, c);
                    }
                }
            }
        }
    }
}
