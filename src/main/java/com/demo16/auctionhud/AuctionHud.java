package com.demo16.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class AuctionHud {
    // Yellow theme (ARGB)
    private static final int BG        = 0xE6120F00;
    private static final int BORDER    = 0xFFFFD21F;
    private static final int TEXT      = 0xFFFFE066;
    private static final int TEXT_DIM  = 0xFFB89B2A;
    private static final int BAR_BG    = 0xFF3A3000;
    private static final int BOX_BG    = 0xFF1E1900;
    private static final int WHITE     = 0xFFFFF4C2;

    private static final int W = 210;
    private static final int H = 108;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AuctionState.visible) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;

        int x = AuctionState.hudX;
        int y = AuctionState.hudY;

        // Panel
        ctx.fill(x, y, x + W, y + H, BG);
        border(ctx, x, y, W, H, BORDER);

        // Header
        ctx.drawItem(AuctionState.stack(), x + 5, y + 5);
        String itemName = AuctionState.stack().getName().getString();
        ctx.drawText(tr, "AUCTION \u00B7 " + AuctionState.quantity + " \u00D7 " + itemName, x + 26, y + 5, TEXT, true);

        String status;
        if (AuctionState.finished) status = "SOLD to " + AuctionState.topBidder;
        else if (!AuctionState.running) status = "stopped";
        else if (AuctionState.topBidder == null) status = "waiting for bids...";
        else status = "going once... " + Math.max(0, (AuctionState.endTime - System.currentTimeMillis()) / 1000 + 1) + "s";
        ctx.drawText(tr, status, x + 26, y + 16, TEXT_DIM, false);

        // LIVE / ENDED badge
        String badge = AuctionState.running ? "LIVE" : (AuctionState.finished ? "SOLD" : "OFF");
        int bw = tr.getWidth(badge) + 8;
        int bx = x + W - bw - 6;
        ctx.fill(bx, y + 5, bx + bw, y + 16, AuctionState.running ? 0xFF4A3D00 : 0xFF2A2500);
        border(ctx, bx, y + 5, bw, 11, BORDER);
        ctx.drawText(tr, badge, bx + 4, y + 7, TEXT, false);

        // Timer bar
        int barX = x + 4, barW = W - 8, barY = y + 28;
        ctx.fill(barX, barY, barX + barW, barY + 3, BAR_BG);
        if (AuctionState.running && AuctionState.topBidder != null) {
            long total = AuctionState.timerSec * 1000L;
            long left = Math.max(0, AuctionState.endTime - System.currentTimeMillis());
            int fill = (int) (barW * (left / (double) Math.max(1, total)));
            ctx.fill(barX, barY, barX + fill, barY + 3, BORDER);
        } else if (AuctionState.finished) {
            ctx.fill(barX, barY, barX + barW, barY + 3, BORDER);
        }

        // Highest bid box
        int bxx = x + 6, byy = y + 36, bww = W - 12, bhh = 32;
        ctx.fill(bxx, byy, bxx + bww, byy + bhh, BOX_BG);
        border(ctx, bxx, byy, bww, bhh, BORDER);
        ctx.drawText(tr, "\u2605 HIGHEST BID", bxx + 5, byy + 4, TEXT, false);
        if (AuctionState.topBidder == null) {
            ctx.drawText(tr, "\u2014", bxx + 5, byy + 18, TEXT_DIM, false);
            String nb = "no bids yet";
            ctx.drawText(tr, nb, bxx + bww - tr.getWidth(nb) - 5, byy + 18, TEXT_DIM, false);
        } else {
            ctx.drawText(tr, AuctionState.topBidder, bxx + 5, byy + 18, WHITE, true);
            String amt = AuctionState.formatMoney(AuctionState.topBid);
            ctx.drawText(tr, amt, bxx + bww - tr.getWidth(amt) - 5, byy + 18, TEXT, true);
        }

        // Worth / min bid
        row(ctx, tr, x + 8, x + W - 8, y + 74, "worth", AuctionState.formatMoney(AuctionState.parseMoney(AuctionState.worthText)));
        row(ctx, tr, x + 8, x + W - 8, y + 86, "min bid", AuctionState.formatMoney(AuctionState.minBid));

        // Footer
        String footer = "demo16's Auction's Hud";
        ctx.drawText(tr, footer, x + (W - tr.getWidth(footer)) / 2, y + H - 10, 0xFF8A7420, false);

        // Session money box
        if (AuctionState.showSession) {
            String s = "SESSION " + AuctionState.formatMoney(AuctionState.sessionMoney);
            int sw = tr.getWidth(s) + 12;
            int sx = x + W + 4;
            ctx.fill(sx, y, sx + sw, y + 16, BG);
            border(ctx, sx, y, sw, 16, BORDER);
            ctx.drawText(tr, s, sx + 6, y + 4, TEXT, true);
        }
    }

    private static void row(DrawContext ctx, TextRenderer tr, int left, int right, int y, String label, String value) {
        ctx.drawText(tr, "\u2022 " + label, left, y, TEXT_DIM, false);
        ctx.drawText(tr, value, right - tr.getWidth(value), y, TEXT, true);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
