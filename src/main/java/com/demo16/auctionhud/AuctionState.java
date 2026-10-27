package com.demo16.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    // ---- winner popup (3 seconds) ----
    public static final long POPUP_MS = 3000;
    public static long popupUntil = 0;
    public static String popupName = null;
    public static String popupItem = "";
    public static long popupBid = 0;
    public static PlayerListEntry popupEntry = null;

    // ---- who is winning right now (for the skin) ----
    public static PlayerListEntry topEntry = null;

    public static boolean debug = false;

    private AuctionState() {}

    /** Timer starts counting RIGHT AWAY when the auction starts. */
    public static void start() {
        visible = true;
        running = true;
        finished = false;
        topBidder = null;
        topBid = 0;
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
        topEntry = null;
        popupUntil = 0;
        endTime = 0;
    }

    /** Shows the winner popup + fireworks right now (for testing / setting things up). */
    public static void testWin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String who = mc.player != null ? mc.player.getName().getString() : "Steve";
        popupName = who;
        popupBid = 5_000_000L;
        popupItem = (quantity > 1 ? quantity + "x " : "") + stack().getName().getString();
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

        MinecraftClient mc = MinecraftClient.getInstance();
        if (running) {
            placeBid(name, amount);
        } else {
            sessionMoney += amount;
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("+" + formatMoney(amount) + " from " + name
                        + "  (session " + formatMoney(sessionMoney) + ")"), true);
            }
        }
    }

    public static void placeBid(String name, long amount) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!running) {
            if (debug) say(mc, "[AuctionHud] payment from " + name + " ignored: no auction running");
            return;
        }
        if (amount < minBid) {
            say(mc, "[AuctionHud] " + name + " paid " + formatMoney(amount) + " (below min bid " + formatMoney(minBid) + ")");
            return;
        }
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
            sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f);
            if (announce && now - lastAnnounce > 2000) {
                lastAnnounce = now;
                chat(mc, "[Auction] " + name + " leads with " + formatMoney(amount) + "!");
            }
        }
    }

    public static void sound(SoundEvent ev, float pitch) {
        if (!sounds) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) mc.player.playSound(ev, 1.0f, pitch);
    }

    /** Sends a real chat message to the server. */
    public static void chat(MinecraftClient mc, String msg) {
        if (mc != null && mc.getNetworkHandler() != null) mc.getNetworkHandler().sendChatMessage(msg);
    }

    /** Call once per client tick. */
    public static void tick() {
        if (running && System.currentTimeMillis() >= closeTime()) {
            running = false;
            finished = true;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (topBidder == null && announce) chat(mc, "[Auction] Ended with no bids.");
            if (topBidder != null) {
                sessionMoney += topBid;
                popupName = topBidder;
                popupBid = topBid;
                popupItem = (quantity > 1 ? quantity + "x " : "") + stack().getName().getString();
                PlayerListEntry fresh = lookup(topBidder);
                popupEntry = fresh != null ? fresh : topEntry;
                popupUntil = System.currentTimeMillis() + POPUP_MS;
                blastsPlayed = 0;
                say(mc, "[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid));
                sound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f);
                if (announce) chat(mc, "[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid) + "!");
            }
        }

        // firework blast sounds, one per burst, timed with the visuals
        long nowMs = System.currentTimeMillis();
        if (winEffect && popupName != null && nowMs < popupUntil) {
            long el = nowMs - (popupUntil - POPUP_MS);
            while (blastsPlayed < Fireworks.BURSTS && el >= (long) (Fireworks.explodeTime(blastsPlayed) * 1000)) {
                sound(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f + 0.1f * (blastsPlayed % 3));
                blastsPlayed++;
            }
        }
    }

    /** Skin of the current leader (looked up again if they weren't in the tab list yet). */
    public static PlayerListEntry leaderEntry() {
        if (topBidder != null && topEntry == null) topEntry = lookup(topBidder);
        return topEntry;
    }

    public static PlayerListEntry lookup(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || name == null) return null;
        return mc.getNetworkHandler().getPlayerListEntry(name);
    }

    public static void say(MinecraftClient mc, String msg) {
        if (mc != null && mc.player != null) mc.player.sendMessage(Text.literal(msg), false);
    }

    public static ItemStack stack() {
        String s = itemId.trim().toLowerCase().replace(' ', '_');
        if (!s.contains(":")) s = "minecraft:" + s;
        Identifier id = Identifier.tryParse(s);
        if (id != null && Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id));
        }
        return new ItemStack(Items.TOTEM_OF_UNDYING);
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
