package com.demo16.auctionhud;

import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.render.RenderTickCounter;

public final class AuctionHud {
    private static final int RED = 0xFFFF5555;
    private static final int ORANGE = 0xFFFFAA00;
    public static final int W = 200;
    public static final int H = 80;

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
        WinnerPopup.render(ctx, font);
    }

    public static int clampX(int x, int sw) { return Math.max(0, Math.min(x, Math.max(0, sw - W))); }
    public static int clampY(int y, int sh) { return Math.max(0, Math.min(y, Math.max(0, sh - H))); }

    /** Draws the panel (if showPanel) and the session pill (if enabled) with the HUD's top-left at x,y. */
    public static void drawAt(DrawContext ctx, TextRenderer font, int x, int y, boolean showPanel) {
        HudStyle s = HudStyle.current();
        if (showPanel) drawPanel(ctx, font, x, y, liveModel(), s);
        if (AuctionState.showSession) {
            int sh = ctx.getScaledWindowHeight();
            int py;
            if (!showPanel) py = y;                                   // alone: sits where the HUD would be
            else py = (y + H + 3 + 16 > sh) ? y - 19 : y + H + 3;     // under the panel (or above if no room)
            drawSessionPill(ctx, font, x, py, s);
        }
    }

    /** Draws a sample panel (fake bid) in the given style. Used by the style picker. */
    public static void drawPreview(DrawContext ctx, TextRenderer font, HudStyle s) {
        drawPanel(ctx, font, 0, 0, previewModel(), s);
    }

    private static void drawSessionPill(DrawContext ctx, TextRenderer font, int x, int y, HudStyle s) {
        String str = "SESSION " + AuctionState.formatMoney(AuctionState.sessionMoney);
        int sw = font.getWidth(str) + 12;
        ctx.fill(x, y, x + sw, y + 16, s.bg);
        Gfx.border(ctx, x, y, sw, 16, s.border);
        ctx.drawText(font, str, x + 6, y + 4, s.accent, s.shadow);
    }

    // =====================================================================
    //  DATA THE PANEL DRAWS (live auction or a fake one for previews)
    // =====================================================================
    private static final class Model {
        ItemStack stack;
        String itemLine = "";
        String pill = "";
        int pillColor = 0;            // 0 = use style colours
        String l2Prefix = "";
        String l2Name = "";
        int l2Color = 0;              // 0 = style "dim"
        float barFrac = 0f;
        int barColor = 0;             // 0 = style bar colour
        boolean hasBid = false;
        String leadLabel = "LEADING";
        String bidder = "";
        long bid = 0;
        PlayerListEntry entry = null;
        boolean flash = false;
        String worth = "";
        String min = "";
    }

    private static String clock(long sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    private static Model liveModel() {
        long now = System.currentTimeMillis();
        Model m = new Model();
        m.stack = AuctionState.stack();
        String itemName = m.stack.getName().getString();
        m.itemLine = "AUCTION \u00B7 " + AuctionState.quantity + " \u00D7 " + itemName;

        long msLeft = Math.max(0, AuctionState.endTime - now);
        long secLeft = (msLeft + 999) / 1000;
        boolean inDelay = AuctionState.running && now >= AuctionState.endTime;
        boolean flashRed = ((now / 250) % 2 == 0);

        if (AuctionState.running) {
            if (inDelay) {
                long delayLeft = (Math.max(0, AuctionState.closeTime() - now) + 999) / 1000;
                m.pill = "FINAL " + delayLeft + "s";
                m.pillColor = ORANGE;
                m.l2Prefix = "Final bids... " + delayLeft + "s";
                m.l2Color = ORANGE;
                long total = Math.max(1, AuctionState.delaySec * 1000L);
                long left = Math.max(0, AuctionState.closeTime() - now);
                m.barFrac = left / (float) total;
                m.barColor = ORANGE;
            } else {
                m.pill = "LIVE " + clock(secLeft);
                if (secLeft <= 5) {
                    m.pillColor = flashRed ? RED : 0xFFFF9999;
                    m.barColor = RED;
                }
                m.barFrac = msLeft / (float) Math.max(1, AuctionState.timerSec * 1000L);
                if (AuctionState.topBidder != null) {
                    m.l2Prefix = "leading ";
                    m.l2Name = AuctionState.topBidder;
                } else {
                    m.l2Prefix = "waiting for bids";
                }
            }
        } else if (AuctionState.finished) {
            m.pill = AuctionState.topBidder == null ? "END" : "SOLD";
            if (AuctionState.topBidder != null) {
                m.l2Prefix = "SOLD to ";
                m.l2Name = AuctionState.topBidder;
            } else {
                m.l2Prefix = "ended - no bids";
            }
        } else {
            m.pill = "OFF";
            m.l2Prefix = AuctionState.endTime == 0 ? "ready - press Start" : "stopped";
        }

        m.hasBid = AuctionState.topBidder != null;
        m.leadLabel = AuctionState.finished ? "WINNER" : "LEADING";
        m.bidder = m.hasBid ? AuctionState.topBidder : "";
        m.bid = AuctionState.topBid;
        m.entry = m.hasBid ? AuctionState.leaderEntry() : null;
        m.flash = now - AuctionState.lastBidTime < 700 && m.hasBid;
        m.worth = AuctionState.formatMoney(AuctionState.parseMoney(AuctionState.worthText));
        m.min = AuctionState.formatMoney(AuctionState.minBid);
        return m;
    }

    private static Model previewModel() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String who = mc.player != null ? mc.player.getName().getString() : "Steve";
        Model m = new Model();
        m.stack = AuctionState.stack();
        m.itemLine = "AUCTION \u00B7 " + AuctionState.quantity + " \u00D7 " + m.stack.getName().getString();
        m.pill = "LIVE 0:25";
        m.l2Prefix = "leading ";
        m.l2Name = who;
        m.barFrac = 0.42f;
        m.hasBid = true;
        m.bidder = who;
        m.bid = 5_000_000L;
        m.entry = AuctionState.lookup(who);
        m.worth = "$200M";
        m.min = "$1M";
        return m;
    }

    // =====================================================================
    //  THE PANEL
    // =====================================================================
    private static void drawPanel(DrawContext ctx, TextRenderer font, int x, int y, Model m, HudStyle s) {
        boolean sh = s.shadow;

        ctx.fill(x, y, x + W, y + H, s.bg);
        Gfx.border(ctx, x, y, W, H, s.border);
        Gfx.border(ctx, x + 2, y + 2, W - 4, H - 4, Gfx.alpha(s.border, 0.22f));

        // ---- pill (top right): LIVE 0:25 ----
        int pillText = m.pillColor != 0 ? m.pillColor : s.accent;
        int pillEdge = m.pillColor != 0 ? m.pillColor : s.border;
        int pw = font.getWidth(m.pill) + 8;
        int px = x + W - pw - 5;
        ctx.fill(px, y + 5, px + pw, y + 16, s.pillBg);
        Gfx.border(ctx, px, y + 5, pw, 11, pillEdge);
        ctx.drawText(font, m.pill, px + 4, y + 7, pillText, false);

        // ---- item icon + "AUCTION · 1 × Name" ----
        ctx.drawItem(m.stack, x + 4, y + 4);
        String header = font.trimToWidth(m.itemLine, W - 24 - pw - 12);
        ctx.drawText(font, header, x + 24, y + 6, s.accent, sh);

        // ---- second line: leading / status ----
        int l2c = m.l2Color != 0 ? m.l2Color : s.dim;
        ctx.drawText(font, m.l2Prefix, x + 24, y + 16, l2c, false);
        if (!m.l2Name.isEmpty()) {
            int off = font.getWidth(m.l2Prefix);
            String nm = font.trimToWidth(m.l2Name, W - 24 - off - 8);
            ctx.drawText(font, nm, x + 24 + off, y + 16, s.who, false);
        }

        // ---- progress bar ----
        int barX = x + 4, barW = W - 8, barY = y + 26;
        ctx.fill(barX, barY, barX + barW, barY + 3, s.barBg);
        int filled = (int) (barW * Gfx.clamp01(m.barFrac));
        if (filled > 0) ctx.fill(barX, barY, barX + filled, barY + 3, m.barColor != 0 ? m.barColor : s.bar);

        // ---- highest bid box: big amount on the left, skin + name on the right ----
        int lx = x + 4, ly = y + 32, lw = W - 8, lh = 26;
        ctx.fill(lx, ly, lx + lw, ly + lh, s.boxBg);
        Gfx.border(ctx, lx, ly, lw, lh, m.flash ? 0xFFFFFFFF : s.boxBorder);

        if (!m.hasBid) {
            String nb = "no bids yet";
            ctx.drawText(font, nb, lx + (lw - font.getWidth(nb)) / 2, ly + 9, s.dim, false);
        } else {
            // skin (right)
            int face = 20;
            int fx = lx + lw - face - 4, fy = ly + 3;
            Gfx.border(ctx, fx - 1, fy - 1, face + 2, face + 2, s.border);
            if (m.entry != null) {
                var skin = m.entry.getSkinTextures();
                PlayerSkinDrawer.draw(ctx, skin, fx, fy, face);
            } else {
                ctx.fill(fx, fy, fx + face, fy + face, s.barBg);
                String letter = m.bidder.substring(0, 1).toUpperCase();
                ctx.drawText(font, letter, fx + face / 2 - font.getWidth(letter) / 2, fy + face / 2 - 4, s.who, sh);
            }

            // label + big amount (left)
            ctx.drawText(font, m.leadLabel, lx + 5, ly + 3, s.dim, false);
            String money = AuctionState.formatMoney(m.bid);
            Gfx.scaled(ctx, font, money, lx + 5, ly + 12, 1.5f, s.money, sh);

            // name (between amount and skin, right aligned to the skin)
            int moneyW = (int) (font.getWidth(money) * 1.5f);
            int avail = Math.max(10, fx - 6 - (lx + 5 + moneyW + 8));
            String nm = font.trimToWidth(m.bidder, avail);
            ctx.drawText(font, nm, fx - 5 - font.getWidth(nm), ly + 9, s.who, sh);
        }

        // ---- footer: worth / min bid ----
        ctx.drawText(font, "\u2022 worth", x + 6, y + 62, s.dim, false);
        ctx.drawText(font, m.worth, x + W - 6 - font.getWidth(m.worth), y + 62, s.accent, false);
        ctx.drawText(font, "\u2022 min bid", x + 6, y + 70, s.dim, false);
        ctx.drawText(font, m.min, x + W - 6 - font.getWidth(m.min), y + 70, s.accent, false);
    }
}
