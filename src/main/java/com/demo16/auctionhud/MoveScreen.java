package com.demo16.auctionhud;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/** Drag the HUD with the mouse to put it EXACTLY where you want. */
public class MoveScreen extends class_437 {
    private final class_437 parent;
    private boolean dragging = false;
    private boolean wasDown = true;
    private boolean first = true;
    private int grabX, grabY;

    public MoveScreen(class_437 parent) {
        super(class_2561.method_43470("Move HUD"));
        this.parent = parent;
    }

    @Override
    protected void method_25426() {
        int cx = this.field_22789 / 2;
        int by = this.field_22790 - 28;
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Center horizontally"), b ->
                AuctionState.hudX = (this.field_22789 - AuctionHud.W) / 2)
                .method_46434(cx - 155, by, 100, 20).method_46431());
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Reset (10, 10)"), b -> {
            AuctionState.hudX = 10;
            AuctionState.hudY = 10;
        }).method_46434(cx - 50, by, 100, 20).method_46431());
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Done"), b -> method_25419())
                .method_46434(cx + 55, by, 100, 20).method_46431());
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);

        int x = AuctionHud.clampX(AuctionState.hudX, this.field_22789);
        int y = AuctionHud.clampY(AuctionState.hudY, this.field_22790);

        // mouse button state (works the same on every Minecraft version)
        long window = this.field_22787.method_22683().method_4490();
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
            x = AuctionHud.clampX(mouseX - grabX, this.field_22789);
            y = AuctionHud.clampY(mouseY - grabY, this.field_22790);
            AuctionState.hudX = x;
            AuctionState.hudY = y;
        }
        wasDown = down;

        AuctionHud.drawAt(ctx, this.field_22793, x, y, true);

        ctx.method_27534(this.field_22793,
                class_2561.method_43470("Drag the HUD anywhere with your mouse"), this.field_22789 / 2, 14, 0xFFFFD21F);
        ctx.method_27534(this.field_22793,
                class_2561.method_43470("X: " + x + "   Y: " + y), this.field_22789 / 2, 28, 0xFFFFE066);
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(parent);
    }
}
