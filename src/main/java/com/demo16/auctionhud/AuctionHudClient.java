package com.demo16.auctionhud;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionHudClient implements ClientModInitializer {
    // Matches chat lines like: "Steve paid you $1,500" or "Steve has sent you $2.5k"
    private static final Pattern PAY = Pattern.compile("(\\w{1,16}) (?:has )?(?:paid|sent) you \\$?([\\d,.]+[kKmMbB]?)");

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.demo16_auction_hud.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.demo16_auction_hud"));

        HudRenderCallback.EVENT.register(AuctionHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) mc.setScreen(new SetupScreen());
            AuctionState.tick();
        });

        // Bids come from "X paid you $Y" chat messages (read-only, nothing is sent).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Matcher m = PAY.matcher(message.getString());
            if (m.find()) AuctionState.placeBid(m.group(1), AuctionState.parseMoney(m.group(2)));
        });

        // Local test commands: /auctionhud bid <player> <amount>, start, stop, clear
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("auctionhud")
                        .then(ClientCommandManager.literal("start").executes(c -> { AuctionState.start(); return 1; }))
                        .then(ClientCommandManager.literal("stop").executes(c -> { AuctionState.stop(); return 1; }))
                        .then(ClientCommandManager.literal("clear").executes(c -> { AuctionState.clear(); return 1; }))
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
}
