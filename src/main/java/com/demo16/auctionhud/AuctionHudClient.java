package com.demo16.auctionhud;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionHudClient implements ClientModInitializer {
    private static final String NUM = "([\\d][\\d,]*(?:\\.\\d+)?\\s?[kKmMbB]?)(?![\\w])";

    // "Steve paid you $1K"  /  "Steve has sent you $1,000"
    private static final Pattern PAY =
            Pattern.compile("(\\w{1,16}) (?:has )?(?:paid|sent|gave) you \\$?" + NUM, Pattern.CASE_INSENSITIVE);
    // "You received $1K from Steve"
    private static final Pattern PAY2 =
            Pattern.compile("(?:received|got) \\$?" + NUM + " from (\\w{1,16})", Pattern.CASE_INSENSITIVE);

    private static final class_304.class_11900 CATEGORY =
            class_304.class_11900.method_74698(class_2960.method_60655("demo16_auction_hud", "main"));
    private static class_304 openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(
                new class_304("key.demo16_auction_hud.open", class_3675.class_307.field_1668, 75 /* K */, CATEGORY));

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                class_2960.method_60655("demo16_auction_hud", "auction_hud"),
                AuctionHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.method_1436()) {
                client.method_1507(new SetupScreen());
            }
            AuctionState.tick();
        });

        ClientReceiveMessageEvents.GAME.register(AuctionHudClient::onMessage);
        // some servers send payment lines as normal chat instead of system messages
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> onMessage(message, false));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("auctionhud")
                        .then(ClientCommandManager.literal("start").executes(c -> { AuctionState.start(); return 1; }))
                        .then(ClientCommandManager.literal("stop").executes(c -> { AuctionState.stop(); return 1; }))
                        .then(ClientCommandManager.literal("clear").executes(c -> { AuctionState.clear(); return 1; }))
                        .then(ClientCommandManager.literal("session")
                                .then(ClientCommandManager.literal("reset").executes(c -> {
                                    AuctionState.sessionMoney = 0;
                                    AuctionState.say(class_310.method_1551(), "[AuctionHud] session money reset");
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("testwin").executes(c -> {
                            AuctionState.testWin();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("debug").executes(c -> {
                            AuctionState.debug = !AuctionState.debug;
                            AuctionState.say(class_310.method_1551(),
                                    "[AuctionHud] debug " + (AuctionState.debug ? "ON" : "OFF"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("bid")
                                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                                .executes(c -> {
                                                    AuctionState.placeBid(
                                                            StringArgumentType.getString(c, "player"),
                                                            AuctionState.parseMoney(StringArgumentType.getString(c, "amount")));
                                                    return 1;
                                                }))))));
    }

    private static final Pattern LOOKS_PAY =
            Pattern.compile("\\b(paid|pay|sent|send|gave|give|received|got|transfer\\w*)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OUTGOING =
            Pattern.compile("^\\s*you\\s+(paid|sent|gave|send|pay)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_DOLLAR = Pattern.compile("\\$\\s?" + NUM);
    private static final Pattern AMOUNT_PLAIN =
            Pattern.compile("(?:you|paid|sent|gave|received|got)\\s+" + NUM, Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN = Pattern.compile("\\b\\w{3,16}\\b");
    private static final java.util.Set<String> STOP = java.util.Set.of(
            "you", "your", "paid", "pay", "sent", "send", "gave", "give", "has", "have", "from", "received",
            "got", "the", "and", "for", "money", "transfer", "transferred", "just", "with", "payment", "balance");

    // DonutSMP (and some other servers) style text with small-caps / fancy unicode letters.
    private static final String SMALL_CAPS = "\u1D00\u0299\u1D04\u1D05\u1D07\uA730\u0262\u029C\u026A\u1D0A\u1D0B\u029F\u1D0D\u0274\u1D0F\u1D18\u01EB\u0280\uA731\u1D1B\u1D1C\u1D20\u1D21x\u028F\u1D22";

    private static String normalize(String in) {
        String t = in.replaceAll("\u00A7.", "").replace('\u00A0', ' ');
        t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(t.length());
        for (char c : t.toCharArray()) {
            int i = SMALL_CAPS.indexOf(c);
            sb.append(i >= 0 && c != 'x' ? (char) ('a' + i) : c);
        }
        return sb.toString().trim();
    }

    private static String lastRaw = "";
    private static long lastRawTime = 0;

    private static void onMessage(class_2561 message, boolean overlay) {
        String raw = normalize(message.getString());
        if (raw.isEmpty()) return;

        // GAME + CHAT can both fire for one line: ignore repeats within 1s
        long now = System.currentTimeMillis();
        if (raw.equals(lastRaw) && now - lastRawTime < 1000) return;
        lastRaw = raw;
        lastRawTime = now;

        class_310 mc = class_310.method_1551();
        boolean looksPay = LOOKS_PAY.matcher(raw).find();
        if (AuctionState.debug && (raw.contains("$") || looksPay)) {
            AuctionState.say(mc, "[AuctionHud debug] saw: " + raw);
        }
        if (!looksPay || OUTGOING.matcher(raw).find()) return;

        String name = null;
        long amount = 0;

        Matcher m = PAY.matcher(raw);
        Matcher m2 = PAY2.matcher(raw);
        if (m.find()) {
            name = m.group(1);
            amount = AuctionState.parseMoney(m.group(2));
        } else if (m2.find()) {
            name = m2.group(2);
            amount = AuctionState.parseMoney(m2.group(1));
        } else {
            // generic fallback: needs an amount + "you"/"received" (so it's incoming)
            String low = raw.toLowerCase();
            if (!(low.contains("you") || low.contains("received") || low.contains("got "))) return;
            Matcher a = AMOUNT_DOLLAR.matcher(raw);
            String amt = null;
            if (a.find()) amt = a.group(1);
            else {
                Matcher b = AMOUNT_PLAIN.matcher(raw);
                if (b.find()) amt = b.group(1);
            }
            if (amt == null) return;
            amount = AuctionState.parseMoney(amt);

            String self = mc.field_1724 != null ? mc.field_1724.method_5477().getString() : "";
            String firstUnknown = null;
            Matcher t = TOKEN.matcher(raw);
            while (t.find()) {
                String tok = t.group();
                if (tok.equalsIgnoreCase(self) || STOP.contains(tok.toLowerCase())) continue;
                if (tok.matches("\\d.*")) continue;
                if (AuctionState.lookup(tok) != null) { name = tok; break; }   // a player who is online
                if (firstUnknown == null) firstUnknown = tok;
            }
            if (name == null) name = firstUnknown;
        }

        if (name == null || amount <= 0) return;

        // during an auction it's a bid; otherwise it just adds to the session money
        AuctionState.onPayment(name, amount);
    }
}
