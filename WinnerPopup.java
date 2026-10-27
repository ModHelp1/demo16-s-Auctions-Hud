package com.demo16.auctionhud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.PlayerSkinDrawer;

/**
 * The "AUCTION WON" screen: big item with sunburst, winner skin portrait, name, price, fireworks + confetti.
 * Colours follow the style chosen in "Choose Style".
 */
public final class WinnerPopup {
    private static final int MONEY_GREEN = 0xFF55FF55;
    private static final int SUB = 0xFFC8C8C8;

    // little twinkles around the card: x, y (relative to the centre), phase
    private static final int[][] TWINKLES = {
            {-92, -58, 0}, {96, -50, 2}, {-120, -8, 4}, {124, 0, 1}, {-84, 46, 3}, {88, 52, 5}, {-30, -78, 6}, {40, -70, 7}
    };

    private WinnerPopup() {}

    public static void render(DrawContext ctx, TextRenderer font) {
        long now = System.currentTimeMillis();
        if (now >= AuctionState.popupUntil || AuctionState.popupName == null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        long el = now - (AuctionState.popupUntil - AuctionState.POPUP_MS);
        float t = el / 1000f;
        float total = AuctionState.POPUP_MS / 1000f;

        float fadeIn = Gfx.clamp01(t / 0.25f);
        float exit = Gfx.clamp01((total - t) / 0.4f);
        float master = Math.min(fadeIn, Gfx.clamp01((total - t) / 0.6f));

        HudStyle st = HudStyle.current();
        int glow = Gfx.opaque(st.glow);

        // darken the world a bit so the show pops
        ctx.fill(0, 0, sw, sh, Gfx.alpha(0xFF000000, 0.5f * master));

        if (AuctionState.winEffect) {
            Fireworks.render(ctx, el, sw, sh, glow, master);
        }

        // card size: fits any gui scale
        float fit = Math.min(1f, (sh - 16f) / 150f);
        fit = Math.min(fit, (sw - 20f) / 270f);
        float pop = Gfx.easeOutBack(Gfx.clamp01(t / 0.5f));
        float outS = exit * exit * (3f - 2f * exit);
        float s = fit * pop * outS;
        if (s < 0.03f) return;

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(sw / 2f, sh / 2f);
        m.scale(s, s);
        content(ctx, font, t, master, glow);
        m.popMatrix();
    }

    private static void content(DrawContext ctx, TextRenderer font, float t, float master, int glow) {
        var m = ctx.getMatrices();
        int white = Gfx.alpha(0xFFFFFFFF, master);
        int nameCol = Gfx.alpha(Gfx.blend(glow, 0xFFFFFFFF, 0.6f), master);

        // ================= item with sunburst =================
        int icx = 0, icy = -52;
        int bob = (int) Math.round(Math.sin(t * 3.0) * 1.5);
        rays(ctx, icx, icy + bob, t, glow, master);
        for (int i = 0; i < 5; i++) {
            Gfx.disc(ctx, icx, icy + bob, 34 - i * 6, Gfx.alpha(glow, 0.08f * master));
        }
        m.pushMatrix();
        m.translate(icx - 20f, icy - 20f + bob);
        m.scale(2.5f, 2.5f);
        ctx.drawItem(AuctionState.stack(), 0, 0);
        m.popMatrix();

        // ================= title:  * AUCTION WON * =================
        String title = "AUCTION WON";
        float titleScale = 1.9f;
        float tw = Gfx.spacedWidth(font, title, titleScale, 1.5f);
        Gfx.spaced(ctx, font, title, 0, -22, titleScale, 1.5f, Gfx.alpha(glow, master), true);
        int starUnit = 2;
        float pulse = 1f + 0.12f * (float) Math.sin(t * 6.0);
        int sCol = Gfx.alpha(glow, master);
        m.pushMatrix();
        m.translate(-tw / 2f - 14f, -14f);
        m.scale(pulse, pulse);
        Gfx.star(ctx, 0, 0, starUnit, sCol);
        m.popMatrix();
        m.pushMatrix();
        m.translate(tw / 2f + 14f, -14f);
        m.scale(pulse, pulse);
        Gfx.star(ctx, 0, 0, starUnit, sCol);
        m.popMatrix();

        // ================= skin portrait + name =================
        String name = AuctionState.popupName;
        int nw = Math.max(1, font.getWidth(name));
        float nameScale = 2.2f;
        if (nw * nameScale > 150f) nameScale = Math.max(1f, 150f / nw);
        float nameW = nw * nameScale;
        int face = 34;
        float gapx = 10f;
        float rowW = face + gapx + nameW;
        int fx = (int) (-rowW / 2f);
        int fy = 4;

        // soft glow behind the portrait, then frame
        for (int i = 4; i >= 1; i--) {
            ctx.fill(fx - 2 - i, fy - 2 - i, fx + face + 2 + i, fy + face + 2 + i, Gfx.alpha(glow, 0.07f * master));
        }
        ctx.fill(fx - 2, fy - 2, fx + face + 2, fy + face + 2, Gfx.alpha(glow, master));
        ctx.fill(fx - 1, fy - 1, fx + face + 1, fy + face + 1, Gfx.alpha(0xFF0A0A0A, master));
        PlayerListEntry e = AuctionState.popupEntry;
        if (e != null) {
            var skin = e.getSkinTextures();
            PlayerSkinDrawer.draw(ctx, skin, fx, fy, face);
        } else if (SkinFetcher.face(name) != null) {
            SkinFetcher.draw(ctx, SkinFetcher.face(name), fx, fy, face, master);
        } else {
            ctx.fill(fx, fy, fx + face, fy + face, Gfx.alpha(0xFF2A2A2A, master));
            String letter = name.substring(0, 1).toUpperCase();
            Gfx.centeredScaled(ctx, font, letter, fx + face / 2f, fy + face / 2f - 9f, 2f, nameCol, true);
        }

        float tx = fx + face + gapx;
        float ty = fy + (face - 9f * nameScale) / 2f;
        Gfx.scaled(ctx, font, name, tx, ty, nameScale, nameCol, true);

        // sparkles at both ends of the row
        float tw1 = 0.5f + 0.5f * (float) Math.sin(t * 7.0);
        float tw2 = 0.5f + 0.5f * (float) Math.sin(t * 7.0 + 2.0);
        Gfx.sparkle(ctx, fx - 14, fy + 8, (int) (4 + 5 * tw1), glow, master * (0.45f + 0.55f * tw1));
        Gfx.sparkle(ctx, (int) (fx + rowW + 14), fy + face - 8, (int) (4 + 5 * tw2), glow, master * (0.45f + 0.55f * tw2));

        // ================= price + details =================
        String won = "won for " + AuctionState.formatMoney(AuctionState.popupBid);
        Gfx.centeredScaled(ctx, font, won, 0, 44, 1.5f, Gfx.alpha(MONEY_GREEN, master), true);

        int bids = Math.max(1, AuctionState.popupBids);
        String sub = AuctionState.popupQty + " \u00D7 " + AuctionState.popupItem
                + " \u00B7 " + bids + (bids == 1 ? " bid" : " bids")
                + " \u00B7 total " + AuctionState.formatMoney(AuctionState.popupTotal);
        float subScale = Math.min(1f, 250f / Math.max(1, font.getWidth(sub)));
        Gfx.centeredScaled(ctx, font, sub, 0, 62, subScale, Gfx.alpha(SUB, master), true);

        // ================= twinkles around the card =================
        for (int[] tw3 : TWINKLES) {
            float f = 0.5f + 0.5f * (float) Math.sin(t * 5.0 + tw3[2] * 1.3);
            Gfx.sparkle(ctx, tw3[0], tw3[1], (int) (2 + 4 * f), white, master * f);
        }
    }

    /** Slowly turning golden rays behind the item. */
    private static void rays(DrawContext ctx, int cx, int cy, float t, int color, float master) {
        var m = ctx.getMatrices();
        int n = 14;
        for (int i = 0; i < n; i++) {
            float ang = (float) (Math.PI * 2 * i / n + t * 0.35);
            float len = (i % 2 == 0) ? 92f : 62f;
            m.pushMatrix();
            m.translate(cx, cy);
            m.rotate(ang);
            for (int k = 0; k < 12; k++) {
                float f = 1f - k / 12f;
                int c = Gfx.alpha(color, 0.38f * f * master);
                int x0 = 14 + (int) (len * k / 12f);
                int x1 = 14 + (int) (len * (k + 1) / 12f);
                int hh = k < 5 ? 2 : 1;
                ctx.fill(x0, -hh, x1, hh, c);
            }
            m.popMatrix();
        }
    }
}
