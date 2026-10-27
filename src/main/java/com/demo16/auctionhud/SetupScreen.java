package com.demo16.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupScreen extends Screen {
    private static final String[] H_NAMES = {"Left", "Center", "Right"};
    private static final String[] V_NAMES = {"Top", "Middle", "Bottom"};

    private TextFieldWidget worth, minBid, quantity, timer, posX, posY;
    private int top;

    public SetupScreen() {
        super(Text.literal("demo16's Auction's Hud"));
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 20, Text.empty());
        f.setMaxLength(64);
        f.setText(value);
        this.addDrawableChild(f);
        return f;
    }

    private static String onOff(boolean b) { return b ? "On" : "Off"; }

    @Override
    protected void init() {
        int left = this.width / 2 - 155;
        int right = left + 160;
        top = Math.max(20, this.height / 2 - 134);

        // ---- item dropdown (opens a picker with search + icons) ----
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(AuctionState.stack().getName().getString() + "  \u25BC"),
                b -> { apply(); this.client.setScreen(new ItemPickerScreen(this)); })
                .dimensions(left, top + 12, 310, 20).build());

        worth    = field(left,  top + 50,  150, AuctionState.worthText);
        minBid   = field(right, top + 50,  150, String.valueOf(AuctionState.minBid));
        quantity = field(left,  top + 88,  150, String.valueOf(AuctionState.quantity));
        timer    = field(right, top + 88,  150, String.valueOf(AuctionState.timerSec));
        posX     = field(left,  top + 126, 150, String.valueOf(AuctionState.hudX));
        posY     = field(right, top + 126, 150, String.valueOf(AuctionState.hudY));

        // ---- HUD position ----
        this.addDrawableChild(ButtonWidget.builder(Text.literal("HUD side: " + H_NAMES[AuctionState.anchorH]), b -> {
            AuctionState.anchorH = (AuctionState.anchorH + 1) % 3;
            b.setMessage(Text.literal("HUD side: " + H_NAMES[AuctionState.anchorH]));
        }).dimensions(left, top + 152, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("HUD height: " + V_NAMES[AuctionState.anchorV]), b -> {
            AuctionState.anchorV = (AuctionState.anchorV + 1) % 3;
            b.setMessage(Text.literal("HUD height: " + V_NAMES[AuctionState.anchorV]));
        }).dimensions(right, top + 152, 150, 20).build());

        // ---- start / stop ----
        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6 Start Auction"), b -> {
            apply();
            AuctionState.start();
        }).dimensions(left, top + 178, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25A0 Stop"), b -> AuctionState.stop())
                .dimensions(right, top + 178, 150, 20).build());

        // ---- toggles ----
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Anti-snipe: " + onOff(AuctionState.antiSnipe)), b -> {
            AuctionState.antiSnipe = !AuctionState.antiSnipe;
            b.setMessage(Text.literal("Anti-snipe: " + onOff(AuctionState.antiSnipe)));
        }).dimensions(left, top + 202, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Sounds: " + onOff(AuctionState.sounds)), b -> {
            AuctionState.sounds = !AuctionState.sounds;
            b.setMessage(Text.literal("Sounds: " + onOff(AuctionState.sounds)));
        }).dimensions(right, top + 202, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Announce in chat: " + onOff(AuctionState.announce)), b -> {
            AuctionState.announce = !AuctionState.announce;
            b.setMessage(Text.literal("Announce in chat: " + onOff(AuctionState.announce)));
        }).dimensions(left, top + 226, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Session money: " + onOff(AuctionState.showSession)), b -> {
            AuctionState.showSession = !AuctionState.showSession;
            b.setMessage(Text.literal("Session money: " + onOff(AuctionState.showSession)));
        }).dimensions(right, top + 226, 150, 20).build());

        // ---- preview / clear ----
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Preview HUD (show/hide)"), b -> {
            apply();
            AuctionState.visible = !AuctionState.visible;
        }).dimensions(left, top + 250, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear HUD & Bids"), b -> AuctionState.clear())
                .dimensions(right, top + 250, 150, 20).build());
    }

    private void apply() {
        if (worth == null) return;
        AuctionState.worthText = worth.getText();
        AuctionState.minBid = AuctionState.parseMoney(minBid.getText());
        AuctionState.quantity = (int) Math.max(1, AuctionState.parseMoney(quantity.getText()));
        AuctionState.timerSec = (int) Math.max(1, AuctionState.parseMoney(timer.getText()));
        AuctionState.hudX = (int) AuctionState.parseMoney(posX.getText());
        AuctionState.hudY = (int) AuctionState.parseMoney(posY.getText());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int left = this.width / 2 - 155;
        int right = left + 160;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top - 14, 0xFFFFD21F);
        ctx.drawText(this.textRenderer, "Item (click to choose)",  left,  top,       0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Worth (10k, 2.5m)",       left,  top + 38,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Min Bid (0 = none)",      right, top + 38,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Quantity",                left,  top + 76,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Timer (seconds, e.g. 60)", right, top + 76, 0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "HUD offset X",            left,  top + 114, 0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "HUD offset Y",            right, top + 114, 0xFFFFE066, false);
        // small icon of the chosen item inside the dropdown button
        ctx.drawItem(AuctionState.stack(), left + 4, top + 14);
    }

    @Override
    public void removed() {
        apply();
    }
}
