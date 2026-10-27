package com.demo16.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** All auction data lives in memory only. Nothing is saved to disk or sent anywhere. */
public class AuctionState {
    public static String itemId = "minecraft:totem_of_undying";
    public static String worthText = "10k";
    public static long minBid = 200;
    public static int quantity = 1;
    public static int timerSec = 5;
    public static int hudX = 10, hudY = 10;
    public static boolean showSession = true;

    public static boolean visible = false;
    public static boolean running = false;
    public static boolean finished = false;
    public static String topBidder = null;
    public static long topBid = 0;
    public static long sessionMoney = 0;
    public static long endTime = 0;

    public static void start() {
        visible = true;
        running = true;
        finished = false;
        topBidder = null;
        topBid = 0;
        endTime = 0;
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
    }

    public static void placeBid(String name, long amount) {
        if (!running) return;
        if (amount < minBid) return;
        if (amount > topBid) {
            topBid = amount;
            topBidder = name;
            endTime = System.currentTimeMillis() + timerSec * 1000L;
        }
    }

    public static void tick() {
        if (running && topBidder != null && System.currentTimeMillis() >= endTime) {
            running = false;
            finished = true;
            sessionMoney += topBid;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("[Auction] SOLD to " + topBidder + " for " + formatMoney(topBid)), false);
            }
        }
    }

    public static ItemStack stack() {
        String s = itemId.trim().toLowerCase().replace(' ', '_');
        if (!s.contains(":")) s = "minecraft:" + s;
        Identifier id = Identifier.tryParse(s);
        if (id != null && Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id));
        }
        return new ItemStack(Items.BARRIER);
    }

    /** Accepts things like 200, $1,500, 10k, 2.5m, 1b. Returns 0 if it can't parse. */
    public static long parseMoney(String in) {
        if (in == null) return 0;
        String s = in.trim().toLowerCase().replace("$", "").replace(",", "");
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
