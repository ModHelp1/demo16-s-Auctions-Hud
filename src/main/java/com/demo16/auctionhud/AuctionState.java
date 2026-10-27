package com.demo16.auctionhud;

import net.minecraft.client.MinecraftClient;
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
    public static int timerSec = 5;
    public static int hudX = 10;
    public static int hudY = 10;
    public static boolean showSession = true;

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
            topBid = amount;
            topBidder = name;
            topEntry = lookup(name);
            // every new top bid resets the countdown
            endTime = System.currentTimeMillis() + timerSec * 1000L;
        }
    }

    /** Call once per client tick. */
    public static void tick() {
        if (running && System.currentTimeMillis() >= endTime) {
            running = false;
            finished = true;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (topBidder != null) {
                sessionMoney += topBid;
                popupName = topBidder;
                popupBid = topBid;
                popupItem = (quantity > 1 ? quantity + "x " : "") + stack().getName().getString();
                PlayerListEntry fresh = lookup(topBidder);
                popupEntry = fresh != null ? fresh : topEntry;
                popupUntil = System.currentTimeMillis() + POPUP_MS;
                say(mc, "[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid));
            }
        }
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
