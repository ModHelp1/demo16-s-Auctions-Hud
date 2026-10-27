package com.demo16.auctionhud;

import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

/** "Choose a Style": grid of live HUD previews. Click one to use it. */
public class StyleScreen extends Screen {
    private static int savedPage = 0;

    private final Screen parent;
    private ButtonWidget[] cells = new ButtonWidget[0];
    private ButtonWidget prev, next;
    private int[] cellX = new int[0], cellY = new int[0];
    private int cardW, cardH, perPage, pages;
    private float ps;
    private int page;

    public StyleScreen(Screen parent) {
        super(Text.literal("Choose a Style"));
        this.parent = parent;
        this.page = savedPage;
    }

    @Override
    protected void init() {
        int sw = this.width, sh = this.height;
        int total = HudStyle.ALL.size();

        // preview size: aim for 4 columns, as big as fits (max 0.8x)
        ps = Math.max(0.45f, Math.min(0.8f, (sw - 52f) / (4f * AuctionHud.W)));
        cardW = (int) (AuctionHud.W * ps);
        cardH = (int) (AuctionHud.H * ps);
        int gap = 8, labelH = 12;
        int cols = Math.max(1, Math.min(4, (sw - 16) / (cardW + gap)));
        int cellH = cardH + labelH + 4;
        int topY = 36;
        int rows = Math.max(1, (sh - topY - 32) / cellH);
        perPage = cols * rows;
        pages = Math.max(1, (total + perPage - 1) / perPage);
        if (page >= pages) page = 0;

        int gridW = cols * cardW + (cols - 1) * gap;
        int gx = (sw - gridW) / 2;

        cells = new ButtonWidget[perPage];
        cellX = new int[perPage];
        cellY = new int[perPage];
        for (int i = 0; i < perPage; i++) {
            final int slot = i;
            cellX[i] = gx + (i % cols) * (cardW + gap);
            cellY[i] = topY + (i / cols) * cellH;
            cells[i] = ButtonWidget.builder(Text.empty(), b -> pick(slot))
                    .dimensions(cellX[i], cellY[i], cardW, cardH).build();
            this.addDrawableChild(cells[i]);
        }

        // bottom row:  < | Test win screen | Done | >
        int by = sh - 24;
        int totalW = 20 + 4 + 110 + 4 + 80 + 4 + 20;
        int bx = (sw - totalW) / 2;
        prev = ButtonWidget.builder(Text.literal("\u25C0"), b -> flip(-1))
                .dimensions(bx, by, 20, 20).build();
        this.addDrawableChild(prev);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Test win screen"), b -> {
            AuctionState.testWin();
            this.client.setScreen(null);
        }).dimensions(bx + 24, by, 110, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(bx + 138, by, 80, 20).build());
        next = ButtonWidget.builder(Text.literal("\u25B6"), b -> flip(1))
                .dimensions(bx + 222, by, 20, 20).build();
        this.addDrawableChild(next);

        refresh();
    }

    private void flip(int d) {
        page = ((page + d) % pages + pages) % pages;
        savedPage = page;
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < perPage; i++) {
            boolean has = page * perPage + i < HudStyle.ALL.size();
            cells[i].visible = has;
            cells[i].active = has;
        }
        boolean multi = pages > 1;
        prev.visible = multi;
        prev.active = multi;
        next.visible = multi;
        next.active = multi;
    }

    private void pick(int slot) {
        int idx = page * perPage + slot;
        if (idx < HudStyle.ALL.size()) AuctionState.styleIndex = idx;
    }

    // no @Override on purpose (same trick as the item picker): works whichever exact signature this version uses
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pages > 1) {
            if (verticalAmount > 0) flip(-1);
            else if (verticalAmount < 0) flip(1);
        }
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;

        Gfx.centered(ctx, this.textRenderer, "Choose a Style", cx, 10, 0xFFFFFFFF, true);
        String sub = "Click a style to use it" + (pages > 1 ? "   (page " + (page + 1) + "/" + pages + ")" : "");
        Gfx.centered(ctx, this.textRenderer, sub, cx, 22, 0xFFA0A0A0, false);

        var m = ctx.getMatrices();
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= HudStyle.ALL.size()) continue;
            HudStyle st = HudStyle.ALL.get(idx);
            int x = cellX[i], y = cellY[i];
            boolean selected = idx == AuctionState.styleIndex;
            boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;

            // live mini HUD
            m.pushMatrix();
            m.translate(x, y);
            m.scale(ps, ps);
            AuctionHud.drawPreview(ctx, this.textRenderer, st);
            m.popMatrix();

            if (selected) {
                Gfx.border(ctx, x - 2, y - 2, cardW + 4, cardH + 4, 0xFFFFFFFF);
                Gfx.border(ctx, x - 1, y - 1, cardW + 2, cardH + 2, 0xFFFFFFFF);
            } else if (hover) {
                Gfx.border(ctx, x - 1, y - 1, cardW + 2, cardH + 2, 0xB0FFFFFF);
            }

            String label = (selected ? "\u2713 " : "") + st.name;
            Gfx.centered(ctx, this.textRenderer, label, x + cardW / 2, y + cardH + 3,
                    selected ? 0xFFFFFFFF : (hover ? 0xFFE0E0E0 : 0xFFA0A0A0), selected);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
