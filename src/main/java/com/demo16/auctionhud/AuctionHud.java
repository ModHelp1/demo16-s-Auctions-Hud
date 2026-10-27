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
    private static final int BAR_BG = 0xFF3A3000;
    private static final int BOX_BG = 0xFF1E1900;
    private static final int W = 210;
    private static final int H = 108;

    private AuctionHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        TextRenderer font = mc.textRenderer;

        renderPanel(ctx, mc, font);
        renderWinnerPopup(ctx, mc, font);
    }

    // =====================================================================
    //  MAIN PANEL
    // =====================================================================
    private static void renderPanel(DrawContext ctx, MinecraftClient mc, TextRenderer font) {
        if (!AuctionState.visible) return;

        int x = AuctionState.hudX, y = AuctionState.hudY;
        long now = System.currentTimeMillis();

        ctx.fill(x, y, x + W, y + H, BG);
        border(ctx, x, y, W, H, BORDER);

        ctx.drawItem(AuctionState.stack(), x + 5, y + 5);
        String itemName = AuctionState.stack().getName().getString();
        ctx.drawText(font, "AUCTION " + AuctionState.quantity + "x " + itemName, x + 26, y + 5, TEXT, true);

        // ---------- status / TIMER LINE ----------
        String status;
        int statusColor = TEXT_DIM;
        long msLeft = Math.max(0, AuctionState.endTime - now);
        long secLeft = (msLeft + 999) / 1000;              // 5,4,3,2,1 – counts every second
        if (AuctionState.finished) {
            status = AuctionState.topBidder != null ? "SOLD to " + AuctionState.topBidder : "ended - no bids";
        } else if (!AuctionState.running) {
            status = "stopped";
        } else {
            status = "Ends in " + secLeft + "s";
            if (secLeft <= 3) statusColor = RED;
        }
        ctx.drawText(font, status, x + 26, y + 16, statusColor, false);

        // ---------- LIVE / SOLD badge ----------
        String badge = AuctionState.running ? "LIVE" : (AuctionState.finished ? "SOLD" : "OFF");
        if (AuctionState.finished && AuctionState.topBidder == null) badge = "END";
        int bw = font.getWidth(badge) + 8;
        int bx = x + W - bw - 6;
        ctx.fill(bx, y + 5, bx + bw, y + 16, AuctionState.running ? 0xFF4A3D00 : 0xFF2A2500);
        border(ctx, bx, y + 5, bw, 11, BORDER);
        ctx.drawText(font, badge, bx + 4, y + 7, TEXT, false);

        // ---------- progress bar ----------
        int barX = x + 4, barW = 202, barY = y + 28;
        ctx.fill(barX, barY, barX + barW, barY + 3, BAR_BG);
        if (AuctionState.running) {
            long total = AuctionState.timerSec * 1000L;
            int filled = (int) (barW * (msLeft / (double) Math.max(1, total)));
            ctx.fill(barX, barY, barX + filled, barY + 3, BORDER);
        }

        // ---------- highest bid box ----------
        int bxx = x + 6, byy = y + 36, boxW = 198, boxH = 32;
        ctx.fill(bxx, byy, bxx + boxW, byy + boxH, BOX_BG);
        border(ctx, bxx, byy, boxW, boxH, BORDER);
        ctx.drawText(font, "\u2605 HIGHEST BID", bxx + 5, byy + 4, TEXT, false);

        if (AuctionState.topBidder == null) {
            ctx.drawText(font, "\u2014", bxx + 5, byy + 18, TEXT_DIM, false);
            String nb = "no bids yet";
            ctx.drawText(font, nb, bxx + boxW - font.getWidth(nb) - 5, byy + 18, TEXT_DIM, false);
        } else {
            // little head of whoever is winning
            int textX = bxx + 5;
            if (AuctionState.topEntry != null) {
                var skin = AuctionState.topEntry.getSkinTextures();
                PlayerSkinDrawer.draw(ctx, skin, bxx + 5, byy + 15, 12);
                textX = bxx + 20;
            }
            ctx.drawText(font, AuctionState.topBidder, textX, byy + 18, WHITE, true);
            String money = AuctionState.formatMoney(AuctionState.topBid);
            ctx.drawText(font, money, bxx + boxW - font.getWidth(money) - 5, byy + 18, TEXT, true);
        }

        row(ctx, font, x + 8, x + W - 8, y + 74, "worth", AuctionState.formatMoney(AuctionState.parseMoney(AuctionState.worthText)));
        row(ctx, font, x + 8, x + W - 8, y + 86, "min bid", AuctionState.formatMoney(AuctionState.minBid));

        String title = "demo16's Auction's Hud";
        ctx.drawText(font, title, x + (W - font.getWidth(title)) / 2, y + H - 10, 0xFF8A7420, false);

        if (AuctionState.showSession) {
            String s = "SESSION " + AuctionState.formatMoney(AuctionState.sessionMoney);
            int sw = font.getWidth(s) + 12;
            int sx = x + W + 4;
            ctx.fill(sx, y, sx + sw, y + 16, BG);
            border(ctx, sx, y, sw, 16, BORDER);
            ctx.drawText(font, s, sx + 6, y + 4, TEXT, true);
        }
    }

    // =====================================================================
    //  BIG WINNER POPUP  (3 seconds)   "[Username] Won the [Item]"
    // =====================================================================
    private static void renderWinnerPopup(DrawContext ctx, MinecraftClient mc, TextRenderer font) {
        long now = System.currentTimeMillis();
        if (now >= AuctionState.popupUntil || AuctionState.popupName == null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        String line = AuctionState.popupName + " Won the " + AuctionState.popupItem;
        String sub = "for " + AuctionState.formatMoney(AuctionState.popupBid);

        // text scale: as big as possible but always fits on screen
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
        border(ctx, bx, by, boxW, boxH, BORDER);
        border(ctx, bx + 2, by + 2, boxW - 4, boxH - 4, 0xFF8A7420);

        // --- head of the winner ---
        int hx = bx + pad, hy = by + pad;
        PlayerListEntry e = AuctionState.popupEntry;
        if (e != null) {
            var skin = e.getSkinTextures();
            PlayerSkinDrawer.draw(ctx, skin, hx, hy, headSize);
        } else {
            // player isn't in the tab list (left / offline): show a letter box instead
            ctx.fill(hx, hy, hx + headSize, hy + headSize, 0xFF3A3000);
            border(ctx, hx, hy, headSize, headSize, BORDER);
            String letter = AuctionState.popupName.substring(0, 1).toUpperCase();
            ctx.drawText(font, letter, hx + headSize / 2 - font.getWidth(letter) / 2, hy + headSize / 2 - 4, WHITE, true);
        }

        // --- big text: "[Username] Won the [Item]" ---
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

    private static void row(DrawContext ctx, TextRenderer font, int left, int right, int y, String label, String value) {
        ctx.drawText(font, label, left, y, TEXT_DIM, false);
        ctx.drawText(font, value, right - font.getWidth(value), y, TEXT, true);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
