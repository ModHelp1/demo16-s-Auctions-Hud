package com.demo16.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;

public final class AuctionHud {
    private static final int BG = 0xE6120F00;
    private static final int BORDER = 0xFFFFD21F;
    private static final int TEXT = 0xFFFFE066;
    private static final int TEXT_DIM = 0xFFB89B2A;
    private static final int WHITE = 0xFFFFF4C2;
    private static final int RED = 0xFFFF5555;
    private static final int ORANGE = 0xFFFFAA00;
    private static final int BAR_BG = 0xFF3A3000;
    private static final int BOX_BG = 0xFF1E1900;
    public static final int W = 180;
    public static final int H = 68;

    private AuctionHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        TextRenderer font = mc.textRenderer;

        // the "move HUD" screen draws its own copy
        if (!(mc.currentScreen instanceof MoveScreen)) {
            int x = clampX(AuctionState.hudX, ctx.getScaledWindowWidth());
            int y = clampY(AuctionState.hudY, ctx.getScaledWindowHeight());
            drawAt(ctx, font, x, y, AuctionState.visible);
        }
        renderWinnerPopup(ctx, font);
    }

    public static int clampX(int x, int sw) { return Math.max(0, Math.min(x, Math.max(0, sw - W))); }
    public static int clampY(int y, int sh) { return Math.max(0, Math.min(y, Math.max(0, sh - H))); }

    /** Draws the panel (if showPanel) and the session pill (if enabled) with the HUD's top-left at x,y. */
    public static void drawAt(DrawContext ctx, TextRenderer font, int x, int y, boolean showPanel) {
        if (showPanel) drawPanel(ctx, font, x, y);
        if (AuctionState.showSession) {
            int sh = ctx.getScaledWindowHeight();
            int py;
            if (!showPanel) py = y;                                   // alone: sits where the HUD would be
            else py = (y + H + 3 + 16 > sh) ? y - 19 : y + H + 3;     // under the panel (or above if no room)
            drawSessionPill(ctx, font, x, py);
        }
    }

    private static String fmtTime(long sec) {
        return sec >= 60 ? (sec / 60) + ":" + String.format("%02d", sec % 60) : sec + "s";
    }

    private static void drawSessionPill(DrawContext ctx, TextRenderer font, int x, int y) {
        String s = "SESSION " + AuctionState.formatMoney(AuctionState.sessionMoney);
        int sw = font.getWidth(s) + 12;
        ctx.fill(x, y, x + sw, y + 16, BG);
        border(ctx, x, y, sw, 16, BORDER);
        ctx.drawText(font, s, x + 6, y + 4, TEXT, true);
    }

    // =====================================================================
    //  SMALL MAIN PANEL
    // =====================================================================
    private static void drawPanel(DrawContext ctx, TextRenderer font, int x, int y) {
        long now = System.currentTimeMillis();

        ctx.fill(x, y, x + W, y + H, BG);
        border(ctx, x, y, W, H, BORDER);

        // ---- badge (top right) ----
        String badge = AuctionState.running ? "LIVE" : (AuctionState.finished ? (AuctionState.topBidder == null ? "END" : "SOLD") : "OFF");
        int bw = font.getWidth(badge) + 8;
        int bx = x + W - bw - 5;
        ctx.fill(bx, y + 5, bx + bw, y + 16, AuctionState.running ? 0xFF4A3D00 : 0xFF2A2500);
        border(ctx, bx, y + 5, bw, 11, BORDER);
        ctx.drawText(font, badge, bx + 4, y + 7, TEXT, false);

        // ---- item icon + name ----
        ctx.drawItem(AuctionState.stack(), x + 4, y + 4);
        String itemName = AuctionState.quantity + "x " + AuctionState.stack().getName().getString();
        itemName = font.trimToWidth(itemName, W - 26 - bw - 10);
        ctx.drawText(font, itemName, x + 24, y + 5, TEXT, true);

        // ---- TIMER ----
        long msLeft = Math.max(0, AuctionState.endTime - now);
        long secLeft = (msLeft + 999) / 1000;
        boolean inDelay = AuctionState.running && now >= AuctionState.endTime;
        String status;
        int statusColor = TEXT_DIM;
        if (AuctionState.finished) {
            status = AuctionState.topBidder != null ? "SOLD to " + AuctionState.topBidder : "ended - no bids";
        } else if (!AuctionState.running) {
            status = AuctionState.endTime == 0 ? "ready - press Start" : "stopped";
        } else if (inDelay) {
            long delayLeft = (Math.max(0, AuctionState.closeTime() - now) + 999) / 1000;
            status = "Final bids... " + delayLeft + "s";
            statusColor = ORANGE;
        } else {
            status = "Ends in " + fmtTime(secLeft);
            if (secLeft <= 5) statusColor = ((now / 250) % 2 == 0) ? RED : 0xFFFF9999;
        }
        ctx.drawText(font, status, x + 24, y + 16, statusColor, false);

        // ---- progress bar ----
        int barX = x + 4, barW = W - 8, barY = y + 26;
        ctx.fill(barX, barY, barX + barW, barY + 3, BAR_BG);
        if (AuctionState.running) {
            if (inDelay) {
                long total = Math.max(1, AuctionState.delaySec * 1000L);
                long left = Math.max(0, AuctionState.closeTime() - now);
                int filled = (int) (barW * (left / (double) total));
                ctx.fill(barX, barY, barX + filled, barY + 3, ORANGE);
            } else {
                long total = AuctionState.timerSec * 1000L;
                int filled = (int) (barW * (msLeft / (double) Math.max(1, total)));
                ctx.fill(barX, barY, barX + filled, barY + 3, secLeft <= 5 ? RED : BORDER);
            }
        }

        // ---- COMPACT LEADER ROW: head + IGN + amount ----
        int lx = x + 4, ly = y + 32, lw = W - 8, lh = 20;
        boolean flash = now - AuctionState.lastBidTime < 700 && AuctionState.topBidder != null;
        ctx.fill(lx, ly, lx + lw, ly + lh, BOX_BG);
        border(ctx, lx, ly, lw, lh, flash ? 0xFFFFFFFF : BORDER);

        if (AuctionState.topBidder == null) {
            String nb = "no bids yet";
            ctx.drawText(font, nb, lx + (lw - font.getWidth(nb)) / 2, ly + 6, TEXT_DIM, false);
        } else {
            int textX = lx + 5;
            PlayerListEntry e = AuctionState.leaderEntry();
            if (e != null) {
                var skin = e.getSkinTextures();
                PlayerSkinDrawer.draw(ctx, skin, lx + 3, ly + 3, 14);
                textX = lx + 21;
            }
            ctx.drawText(font, AuctionState.topBidder, textX, ly + 6, WHITE, true);
            String money = AuctionState.formatMoney(AuctionState.topBid);
            ctx.drawText(font, money, lx + lw - font.getWidth(money) - 5, ly + 6, TEXT, true);
        }

        // ---- info line ----
        String worth = "worth " + AuctionState.formatMoney(AuctionState.parseMoney(AuctionState.worthText));
        String min = "min " + AuctionState.formatMoney(AuctionState.minBid);
        ctx.drawText(font, worth, x + 6, y + 57, TEXT_DIM, false);
        ctx.drawText(font, min, x + W - 6 - font.getWidth(min), y + 57, TEXT_DIM, false);
    }

    // =====================================================================
    //  BIG WINNER POPUP (3 seconds)  "[Username] Won the [Item]!!!"
    // =====================================================================
    private static void renderWinnerPopup(DrawContext ctx, TextRenderer font) {
        long now = System.currentTimeMillis();
        if (now >= AuctionState.popupUntil || AuctionState.popupName == null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        if (AuctionState.winEffect) {
            Fireworks.render(ctx, now - (AuctionState.popupUntil - AuctionState.POPUP_MS), sw, sh);
        }

        String line = AuctionState.popupName + " Won the " + AuctionState.popupItem + "!!!";
        String sub = "for " + AuctionState.formatMoney(AuctionState.popupBid);

        int headSize = 48;
        int pad = 14;
        float maxTextW = Math.min(sw - 40 - headSize - pad * 3, 420);
        float scale = Math.min(2.0f, maxTextW / Math.max(1, font.getWidth(line)));
        scale = Math.max(0.8f, scale);

        int textW = (int) (font.getWidth(line) * scale);
        int boxW = pad + headSize + pad + textW + pad;
        int boxH = headSize + pad * 2;
        int bx = (sw - boxW) / 2;
        int by = sh / 3 - boxH / 2;

        ctx.fill(bx, by, bx + boxW, by + boxH, 0xF0120F00);
        boolean pulse = (now / 150) % 2 == 0;
        border(ctx, bx, by, boxW, boxH, pulse ? BORDER : 0xFFFFFFFF);
        border(ctx, bx + 2, by + 2, boxW - 4, boxH - 4, pulse ? 0xFFFFFFFF : BORDER);

        int hx = bx + pad, hy = by + pad;
        PlayerListEntry e = AuctionState.popupEntry;
        if (e != null) {
            var skin = e.getSkinTextures();
            PlayerSkinDrawer.draw(ctx, skin, hx, hy, headSize);
        } else {
            ctx.fill(hx, hy, hx + headSize, hy + headSize, 0xFF3A3000);
            border(ctx, hx, hy, headSize, headSize, BORDER);
            String letter = AuctionState.popupName.substring(0, 1).toUpperCase();
            ctx.drawText(font, letter, hx + headSize / 2 - font.getWidth(letter) / 2, hy + headSize / 2 - 4, WHITE, true);
        }

        int tx = hx + headSize + pad;
        float lineH = 9 * scale;
        float ty = by + (boxH - lineH - 12) / 2f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(tx, ty);
        m.scale(scale, scale);
        ctx.drawText(font, line, 0, 0, WHITE, true);
        m.popMatrix();

        ctx.drawText(font, sub, tx, (int) (ty + lineH + 4), TEXT, true);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
