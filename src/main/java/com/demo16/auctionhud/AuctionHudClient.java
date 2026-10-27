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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("demo16_auction_hud", "main"));
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.demo16_auction_hud.open", InputUtil.Type.KEYSYM, 75 /* K */, CATEGORY));

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("demo16_auction_hud", "auction_hud"),
                AuctionHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new SetupScreen());
            }
            AuctionState.tick();
        });

        ClientReceiveMessageEvents.GAME.register(AuctionHudClient::onMessage);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("auctionhud")
                        .then(ClientCommandManager.literal("start").executes(c -> { AuctionState.start(); return 1; }))
                        .then(ClientCommandManager.literal("stop").executes(c -> { AuctionState.stop(); return 1; }))
                        .then(ClientCommandManager.literal("clear").executes(c -> { AuctionState.clear(); return 1; }))
                        .then(ClientCommandManager.literal("debug").executes(c -> {
                            AuctionState.debug = !AuctionState.debug;
                            AuctionState.say(MinecraftClient.getInstance(),
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

    private static void onMessage(Text message, boolean overlay) {
        // strip any section-sign colour codes just in case
        String raw = message.getString().replaceAll("\u00A7.", "");

        if (AuctionState.debug && (raw.contains("$") || raw.toLowerCase().contains("paid"))) {
            AuctionState.say(MinecraftClient.getInstance(), "[AuctionHud debug] saw: " + raw);
        }

        Matcher m = PAY.matcher(raw);
        if (m.find()) {
            AuctionState.placeBid(m.group(1), AuctionState.parseMoney(m.group(2)));
            return;
        }
        Matcher m2 = PAY2.matcher(raw);
        if (m2.find()) {
            AuctionState.placeBid(m2.group(2), AuctionState.parseMoney(m2.group(1)));
        }
    }
}
