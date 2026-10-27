package com.demo16.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupScreen extends Screen {
    private TextFieldWidget item, worth, minBid, quantity, timer, posX, posY;
    private ButtonWidget sessionBtn;

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

    @Override
    protected void init() {
        int left = this.width / 2 - 155;
        int right = left + 160;
        int top = this.height / 2 - 100;

        item     = field(left,  top + 12,  310, AuctionState.itemId);
        worth    = field(left,  top + 50,  150, AuctionState.worthText);
        minBid   = field(right, top + 50,  150, String.valueOf(AuctionState.minBid));
        quantity = field(left,  top + 88,  150, String.valueOf(AuctionState.quantity));
        timer    = field(right, top + 88,  150, String.valueOf(AuctionState.timerSec));
        posX     = field(left,  top + 126, 150, String.valueOf(AuctionState.hudX));
        posY     = field(right, top + 126, 150, String.valueOf(AuctionState.hudY));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6 Start Auction"), b -> {
            apply();
            AuctionState.start();
        }).dimensions(left, top + 156, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25A0 Stop"), b -> AuctionState.stop())
                .dimensions(right, top + 156, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear HUD & Bids"), b -> AuctionState.clear())
                .dimensions(left, top + 180, 150, 20).build());

        sessionBtn = ButtonWidget.builder(Text.literal("Session money: " + (AuctionState.showSession ? "On" : "Off")), b -> {
            AuctionState.showSession = !AuctionState.showSession;
            b.setMessage(Text.literal("Session money: " + (AuctionState.showSession ? "On" : "Off")));
        }).dimensions(right, top + 180, 150, 20).build();
        this.addDrawableChild(sessionBtn);
    }

    private void apply() {
        AuctionState.itemId = item.getText();
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
        int top = this.height / 2 - 100;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top - 14, 0xFFFFD21F);
        ctx.drawText(this.textRenderer, "Item (e.g. totem_of_undying)", left,  top,       0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Worth (10k, 2.5m)",            left,  top + 38,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Min Bid (0 = none)",           right, top + 38,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Quantity",                     left,  top + 76,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "Timer (sec)",                  right, top + 76,  0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "HUD X",                        left,  top + 114, 0xFFFFE066, false);
        ctx.drawText(this.textRenderer, "HUD Y",                        right, top + 114, 0xFFFFE066, false);
    }

    @Override
    public void removed() {
        apply();
    }
}
