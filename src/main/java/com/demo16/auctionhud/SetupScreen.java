package com.demo16.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupScreen extends Screen {
    private TextFieldWidget item, worth, minBid, qty, timer, posX, posY;

    public SetupScreen() {
        super(Text.literal("demo16's Auction's Hud"));
    }

    private TextFieldWidget field(int x, int y, int w, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.empty());
        f.setMaxLength(64);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int top = height / 2 - 100;

        item    = field(left, top + 12, 310, AuctionState.itemId);
        worth   = field(left, top + 50, 150, AuctionState.worthText);
        minBid  = field(left + 160, top + 50, 150, String.valueOf(AuctionState.minBid));
        qty     = field(left, top + 88, 150, String.valueOf(AuctionState.quantity));
        timer   = field(left + 160, top + 88, 150, String.valueOf(AuctionState.timerSec));
        posX    = field(left, top + 126, 150, String.valueOf(AuctionState.hudX));
        posY    = field(left + 160, top + 126, 150, String.valueOf(AuctionState.hudY));

        addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6 Start Auction"), b -> { apply(); AuctionState.start(); })
                .dimensions(left, top + 156, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("\u25A0 Stop"), b -> AuctionState.stop())
                .dimensions(left + 160, top + 156, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear HUD & Bids"), b -> AuctionState.clear())
                .dimensions(left, top + 180, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Session money: " + (AuctionState.showSession ? "On" : "Off")), b -> {
            AuctionState.showSession = !AuctionState.showSession;
            b.setMessage(Text.literal("Session money: " + (AuctionState.showSession ? "On" : "Off")));
        }).dimensions(left + 160, top + 180, 150, 20).build());
    }

    private void apply() {
        AuctionState.itemId = item.getText();
        AuctionState.worthText = worth.getText();
        AuctionState.minBid = AuctionState.parseMoney(minBid.getText());
        AuctionState.quantity = (int) Math.max(1, AuctionState.parseMoney(qty.getText()));
        AuctionState.timerSec = (int) Math.max(1, AuctionState.parseMoney(timer.getText()));
        AuctionState.hudX = (int) AuctionState.parseMoney(posX.getText());
        AuctionState.hudY = (int) AuctionState.parseMoney(posY.getText());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int left = width / 2 - 155;
        int top = height / 2 - 100;
        int c = 0xFFFFD21F;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 14, c);
        ctx.drawText(textRenderer, "Item (e.g. totem_of_undying)", left, top, c, false);
        ctx.drawText(textRenderer, "Worth (10k, 2.5m)", left, top + 38, c, false);
        ctx.drawText(textRenderer, "Min Bid (0 = none)", left + 160, top + 38, c, false);
        ctx.drawText(textRenderer, "Quantity", left, top + 76, c, false);
        ctx.drawText(textRenderer, "Timer (sec)", left + 160, top + 76, c, false);
        ctx.drawText(textRenderer, "HUD X", left, top + 114, c, false);
        ctx.drawText(textRenderer, "HUD Y", left + 160, top + 114, c, false);
    }

    @Override
    public void removed() {
        apply(); // keep values when the screen closes
    }
}
