package com.demo16.auctionhud;

import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_640;
import net.minecraft.class_7923;

public final class AuctionState {
    // ---- settings ----
    public static String itemId = "minecraft:totem_of_undying";
    public static String worthText = "10k";
    public static long minBid = 200;
    public static int quantity = 1;
    public static int timerSec = 60;
    public static int hudX = 10;
    public static int hudY = 10;
    public static boolean showSession = true;
    public static int delaySec = 0;         // stream delay: bids still count this long AFTER the timer hits 0
    public static boolean antiSnipe = false; // bid in last N sec -> timer goes back up to N sec
    public static int snipeSec = 5;
    public static boolean sounds = true;
    public static boolean winEffect = true;  // fireworks when someone wins
    public static int styleIndex = HudStyle.defaultIndex();  // chosen HUD style (see Choose Style)
    public static int blastsPlayed = 0;
    public static boolean announce = false;  // say bids / winner in public chat
    public static long lastBidTime = 0;
    public static long lastAnnounce = 0;
    public static String lastPayKey = "";
    public static long lastPayTime = 0;

    // ---- live state ----
    public static boolean visible = false;
    public static boolean running = false;
    public static boolean finished = false;
    public static String topBidder = null;
    public static long topBid = 0;
    public static long sessionMoney = 0;
    public static long endTime = 0;
    public static int bidCount = 0;

    // ---- winner screen (5 seconds) ----
    public static final long POPUP_MS = 5000;
    public static long popupUntil = 0;
    public static String popupName = null;
    public static String popupItem = "";   // item name only
    public static int popupQty = 1;
    public static int popupBids = 1;
    public static long popupTotal = 0;      // session total after this sale
    public static long popupBid = 0;
    public static class_640 popupEntry = null;

    // ---- who is winning right now (for the skin) ----
    public static class_640 topEntry = null;

    public static boolean debug = false;

    private AuctionState() {}

    /** Timer starts counting RIGHT AWAY when the auction starts. */
    public static void start() {
        visible = true;
        running = true;
        finished = false;
        topBidder = null;
        topBid = 0;
        bidCount = 0;
        topEntry = null;
        popupUntil = 0;
        endTime = System.currentTimeMillis() + timerSec * 1000L;
    }

    public static void stop() {
        running = false;
    }

    public static void clear() {
        running = false;
        finished = false;
        visible = false;
        topBidder = null;
        topBid = 0;
        bidCount = 0;
        topEntry = null;
        popupUntil = 0;
        endTime = 0;
    }

    /** Shows the winner popup + fireworks right now (for testing / setting things up). */
    public static void testWin() {
        class_310 mc = class_310.method_1551();
        String who = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "Steve";
        popupName = who;
        popupBid = 5_000_000L;
        popupItem = stack().method_7964().getString();
        popupQty = quantity;
        popupBids = 3;
        popupTotal = Math.max(sessionMoney, popupBid);
        popupEntry = lookup(who);
        popupUntil = System.currentTimeMillis() + POPUP_MS;
        blastsPlayed = 0;
    }

    /** When the auction really closes = timer end + stream delay. */
    public static long closeTime() {
        return endTime + delaySec * 1000L;
    }

    /** Any incoming payment. Counts as a bid during an auction, otherwise goes straight to session money. */
    public static void onPayment(String name, long amount) {
        long now = System.currentTimeMillis();
        String key = name.toLowerCase() + "|" + amount;
        // DonutSMP shows the same payment in chat AND the action bar: only count it once
        if (key.equals(lastPayKey) && now - lastPayTime < 2000) return;
        lastPayKey = key;
        lastPayTime = now;

        class_310 mc = class_310.method_1551();
        if (running) {
            placeBid(name, amount);
        } else {
            sessionMoney += amount;
            if (mc.field_1724 != null) {
                mc.field_1724.method_7353(class_2561.method_43470("+" + formatMoney(amount) + " from " + name
                        + "  (session " + formatMoney(sessionMoney) + ")"), true);
            }
        }
    }

    public static void placeBid(String name, long amount) {
        class_310 mc = class_310.method_1551();
        if (!running) {
            if (debug) say(mc, "[AuctionHud] payment from " + name + " ignored: no auction running");
            return;
        }
        if (amount < minBid) {
            say(mc, "[AuctionHud] " + name + " paid " + formatMoney(amount) + " (below min bid " + formatMoney(minBid) + ")");
            return;
        }
        bidCount++;
        if (amount > topBid) {
            long now = System.currentTimeMillis();
            topBid = amount;
            topBidder = name;
            topEntry = lookup(name);
            lastBidTime = now;
            // The countdown is NOT reset by new bids. Only the optional anti-snipe can add a few seconds.
            if (antiSnipe && endTime - now < snipeSec * 1000L) {
                endTime = now + snipeSec * 1000L;
            }
            sound(class_3417.field_14627, 1.2f);
            if (announce && now - lastAnnounce > 2000) {
                lastAnnounce = now;
                chat(mc, "[Auction] " + name + " leads with " + formatMoney(amount) + "!");
            }
        }
    }

    public static void sound(class_3414 ev, float pitch) {
        if (!sounds) return;
        class_310 mc = class_310.method_1551();
        if (mc != null && mc.field_1724 != null) mc.field_1724.method_5783(ev, 1.0f, pitch);
    }

    /** Sends a real chat message to the server. */
    public static void chat(class_310 mc, String msg) {
        if (mc != null && mc.method_1562() != null) mc.method_1562().method_45729(msg);
    }

    /** Call once per client tick. */
    public static void tick() {
        if (running && System.currentTimeMillis() >= closeTime()) {
            running = false;
            finished = true;
            class_310 mc = class_310.method_1551();
            if (topBidder == null && announce) chat(mc, "[Auction] Ended with no bids.");
            if (topBidder != null) {
                sessionMoney += topBid;
                popupName = topBidder;
                popupBid = topBid;
                popupItem = stack().method_7964().getString();
                popupQty = quantity;
                popupBids = Math.max(1, bidCount);
                popupTotal = sessionMoney;
                class_640 fresh = lookup(topBidder);
                popupEntry = fresh != null ? fresh : topEntry;
                popupUntil = System.currentTimeMillis() + POPUP_MS;
                blastsPlayed = 0;
                say(mc, "[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid));
                sound(class_3417.field_14709, 1.0f);
                if (announce) chat(mc, "[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid) + "!");
            }
        }

        // firework blast sounds, one per burst, timed with the visuals
        long nowMs = System.currentTimeMillis();
        if (winEffect && popupName != null && nowMs < popupUntil) {
            long el = nowMs - (popupUntil - POPUP_MS);
            while (blastsPlayed < Fireworks.BURSTS && el >= (long) (Fireworks.explodeTime(blastsPlayed) * 1000)) {
                sound(class_3417.field_14917, 0.9f + 0.1f * (blastsPlayed % 3));
                blastsPlayed++;
            }
        }
    }

    /** Skin of the current leader (looked up again if they weren't in the tab list yet). */
    public static class_640 leaderEntry() {
        if (topBidder != null && topEntry == null) topEntry = lookup(topBidder);
        return topEntry;
    }

    public static class_640 lookup(String name) {
        class_310 mc = class_310.method_1551();
        if (mc.method_1562() == null || name == null) return null;
        return mc.method_1562().method_2874(name);
    }

    public static void say(class_310 mc, String msg) {
        if (mc != null && mc.field_1724 != null) mc.field_1724.method_7353(class_2561.method_43470(msg), false);
    }

    public static class_1799 stack() {
        String s = itemId.trim().toLowerCase().replace(' ', '_');
        if (!s.contains(":")) s = "minecraft:" + s;
        class_2960 id = class_2960.method_12829(s);
        if (id != null && class_7923.field_41178.method_10250(id)) {
            return new class_1799(class_7923.field_41178.method_63535(id));
        }
        return new class_1799(class_1802.field_8288);
    }

    public static long parseMoney(String s) {
        if (s == null) return 0;
        s = s.trim().toLowerCase().replace("$", "").replace(",", "").replace(" ", "");
        if (s.isEmpty()) return 0;
        double mult = 1;
        char last = s.charAt(s.length() - 1);
        if (last == 'k') { mult = 1_000; s = s.substring(0, s.length() - 1); }
        else if (last == 'm') { mult = 1_000_000; s = s.substring(0, s.length() - 1); }
        else if (last == 'b') { mult = 1_000_000_000; s = s.substring(0, s.length() - 1); }
        try {
            return (long) (Double.parseDouble(s) * mult);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatMoney(long v) {
        if (v >= 1_000_000_000L) return "$" + trim(v / 1_000_000_000.0) + "B";
        if (v >= 1_000_000L) return "$" + trim(v / 1_000_000.0) + "M";
        if (v >= 1_000L) return "$" + trim(v / 1_000.0) + "K";
        return "$" + v;
    }

    private static String trim(double d) {
        String s = String.format("%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
