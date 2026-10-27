package com.demo16.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Drag the HUD with the mouse to put it EXACTLY where you want. */
public class MoveScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private boolean wasDown = true;
    private boolean first = true;
    private int grabX, grabY;

    public MoveScreen(Screen parent) {
        super(Text.literal("Move HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Center horizontally"), b ->
                AuctionState.hudX = (this.width - AuctionHud.W) / 2)
                .dimensions(cx - 155, by, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset (10, 10)"), b -> {
            AuctionState.hudX = 10;
            AuctionState.hudY = 10;
        }).dimensions(cx - 50, by, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx + 55, by, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int x = AuctionHud.clampX(AuctionState.hudX, this.width);
        int y = AuctionHud.clampY(AuctionState.hudY, this.height);

        // mouse button state (works the same on every Minecraft version)
        long window = this.client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (first) { wasDown = down; first = false; }

        boolean inside = mouseX >= x && mouseX < x + AuctionHud.W && mouseY >= y && mouseY < y + AuctionHud.H;
        if (down && !wasDown && inside) {
            dragging = true;
            grabX = mouseX - x;
            grabY = mouseY - y;
        }
        if (!down) dragging = false;
        if (dragging) {
            x = AuctionHud.clampX(mouseX - grabX, this.width);
            y = AuctionHud.clampY(mouseY - grabY, this.height);
            AuctionState.hudX = x;
            AuctionState.hudY = y;
        }
        wasDown = down;

        AuctionHud.drawAt(ctx, this.textRenderer, x, y, true);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Drag the HUD anywhere with your mouse"), this.width / 2, 14, 0xFFFFD21F);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("X: " + x + "   Y: " + y), this.width / 2, 28, 0xFFFFE066);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
