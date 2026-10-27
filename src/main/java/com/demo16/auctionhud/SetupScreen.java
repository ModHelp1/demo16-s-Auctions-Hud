package com.demo16.auctionhud;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class SetupScreen extends class_437 {
    private class_342 worth, minBid, quantity, timer, delay, posX, posY;
    private int top;

    public SetupScreen() {
        super(class_2561.method_43470("demo16's Auction's Hud"));
    }

    private class_342 field(int x, int y, int w, String value) {
        class_342 f = new class_342(this.field_22793, x, y, w, 20, class_2561.method_43473());
        f.method_1880(64);
        f.method_1852(value);
        this.method_37063(f);
        return f;
    }

    private static String onOff(boolean b) { return b ? "On" : "Off"; }

    @Override
    protected void method_25426() {
        int left = this.field_22789 / 2 - 155;
        int right = left + 160;
        top = Math.max(20, this.field_22790 / 2 - 134);

        // ---- item dropdown (opens a picker with search + icons) ----
        this.method_37063(class_4185.method_46430(
                class_2561.method_43470(AuctionState.stack().method_7964().getString() + "  \u25BC"),
                b -> { apply(); this.field_22787.method_1507(new ItemPickerScreen(this)); })
                .method_46434(left, top + 12, 205, 20).method_46431());

        // ---- Choose Style (grid of HUD themes) ----
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Choose Style"),
                b -> { apply(); this.field_22787.method_1507(new StyleScreen(this)); })
                .method_46434(left + 210, top + 12, 100, 20).method_46431());

        worth    = field(left,  top + 50,  150, AuctionState.worthText);
        minBid   = field(right, top + 50,  150, String.valueOf(AuctionState.minBid));
        quantity = field(left,  top + 88,  150, String.valueOf(AuctionState.quantity));
        timer    = field(right, top + 88,  150, String.valueOf(AuctionState.timerSec));
        delay    = field(left,  top + 126, 150, String.valueOf(AuctionState.delaySec));
        posX     = field(right,      top + 126, 72, String.valueOf(AuctionState.hudX));
        posY     = field(right + 78, top + 126, 72, String.valueOf(AuctionState.hudY));

        // ---- move HUD / preview ----
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Move HUD (drag it)"), b -> {
            apply();
            this.field_22787.method_1507(new MoveScreen(this));
        }).method_46434(left, top + 152, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Preview HUD (show/hide)"), b -> {
            apply();
            AuctionState.visible = !AuctionState.visible;
        }).method_46434(right, top + 152, 150, 20).method_46431());

        // ---- start / stop ----
        this.method_37063(class_4185.method_46430(class_2561.method_43470("\u25B6 Start Auction"), b -> {
            apply();
            AuctionState.start();
        }).method_46434(left, top + 178, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("\u25A0 Stop"), b -> AuctionState.stop())
                .method_46434(right, top + 178, 150, 20).method_46431());

        // ---- toggles ----
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Anti-snipe: " + onOff(AuctionState.antiSnipe)), b -> {
            AuctionState.antiSnipe = !AuctionState.antiSnipe;
            b.method_25355(class_2561.method_43470("Anti-snipe: " + onOff(AuctionState.antiSnipe)));
        }).method_46434(left, top + 202, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Sounds: " + onOff(AuctionState.sounds)), b -> {
            AuctionState.sounds = !AuctionState.sounds;
            b.method_25355(class_2561.method_43470("Sounds: " + onOff(AuctionState.sounds)));
        }).method_46434(right, top + 202, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Announce in chat: " + onOff(AuctionState.announce)), b -> {
            AuctionState.announce = !AuctionState.announce;
            b.method_25355(class_2561.method_43470("Announce in chat: " + onOff(AuctionState.announce)));
        }).method_46434(left, top + 226, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Session money: " + onOff(AuctionState.showSession)), b -> {
            AuctionState.showSession = !AuctionState.showSession;
            b.method_25355(class_2561.method_43470("Session money: " + onOff(AuctionState.showSession)));
        }).method_46434(right, top + 226, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Clear HUD & Bids"), b -> AuctionState.clear())
                .method_46434(left, top + 250, 150, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Win fireworks: " + onOff(AuctionState.winEffect)), b -> {
            AuctionState.winEffect = !AuctionState.winEffect;
            b.method_25355(class_2561.method_43470("Win fireworks: " + onOff(AuctionState.winEffect)));
        }).method_46434(right, top + 250, 150, 20).method_46431());
    }

    private void apply() {
        if (worth == null) return;
        AuctionState.worthText = worth.method_1882();
        AuctionState.minBid = AuctionState.parseMoney(minBid.method_1882());
        AuctionState.quantity = (int) Math.max(1, AuctionState.parseMoney(quantity.method_1882()));
        AuctionState.timerSec = (int) Math.max(1, AuctionState.parseMoney(timer.method_1882()));
        AuctionState.delaySec = (int) Math.max(0, AuctionState.parseMoney(delay.method_1882()));
        AuctionState.hudX = (int) AuctionState.parseMoney(posX.method_1882());
        AuctionState.hudY = (int) AuctionState.parseMoney(posY.method_1882());
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        int left = this.field_22789 / 2 - 155;
        int right = left + 160;
        ctx.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, top - 14, 0xFFFFD21F);
        ctx.method_51433(this.field_22793, "Item (click to choose)",       left,  top,       0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "Worth (10k, 2.5m)",            left,  top + 38,  0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "Min Bid (0 = none)",           right, top + 38,  0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "Quantity",                     left,  top + 76,  0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "Timer (seconds, e.g. 60)",     right, top + 76,  0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "Stream delay (sec, 0 = off)",  left,  top + 114, 0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "HUD X",                        right,      top + 114, 0xFFFFE066, false);
        ctx.method_51433(this.field_22793, "HUD Y",                        right + 78, top + 114, 0xFFFFE066, false);
        // small icon of the chosen item inside the dropdown button
        ctx.method_51427(AuctionState.stack(), left + 4, top + 14);
    }

    @Override
    public void method_25432() {
        apply();
    }
}
