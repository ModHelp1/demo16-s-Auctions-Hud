package com.demo16.auctionhud;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** "Choose a Style": grid of live HUD previews. Click one to use it. */
public class StyleScreen extends class_437 {
    private static int savedPage = 0;

    private final class_437 parent;
    private class_4185[] cells = new class_4185[0];
    private class_4185 prev, next;
    private int[] cellX = new int[0], cellY = new int[0];
    private int cardW, cardH, perPage, pages;
    private float ps;
    private int page;

    public StyleScreen(class_437 parent) {
        super(class_2561.method_43470("Choose a Style"));
        this.parent = parent;
        this.page = savedPage;
    }

    @Override
    protected void method_25426() {
        int sw = this.field_22789, sh = this.field_22790;
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

        cells = new class_4185[perPage];
        cellX = new int[perPage];
        cellY = new int[perPage];
        for (int i = 0; i < perPage; i++) {
            final int slot = i;
            cellX[i] = gx + (i % cols) * (cardW + gap);
            cellY[i] = topY + (i / cols) * cellH;
            cells[i] = class_4185.method_46430(class_2561.method_43473(), b -> pick(slot))
                    .method_46434(cellX[i], cellY[i], cardW, cardH).method_46431();
            this.method_37063(cells[i]);
        }

        // bottom row:  < | Test win screen | Done | >
        int by = sh - 24;
        int totalW = 20 + 4 + 110 + 4 + 80 + 4 + 20;
        int bx = (sw - totalW) / 2;
        prev = class_4185.method_46430(class_2561.method_43470("\u25C0"), b -> flip(-1))
                .method_46434(bx, by, 20, 20).method_46431();
        this.method_37063(prev);
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Test win screen"), b -> {
            AuctionState.testWin();
            this.field_22787.method_1507(null);
        }).method_46434(bx + 24, by, 110, 20).method_46431());
        this.method_37063(class_4185.method_46430(class_2561.method_43470("Done"), b -> method_25419())
                .method_46434(bx + 138, by, 80, 20).method_46431());
        next = class_4185.method_46430(class_2561.method_43470("\u25B6"), b -> flip(1))
                .method_46434(bx + 222, by, 20, 20).method_46431();
        this.method_37063(next);

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
            cells[i].field_22764 = has;
            cells[i].field_22763 = has;
        }
        boolean multi = pages > 1;
        prev.field_22764 = multi;
        prev.field_22763 = multi;
        next.field_22764 = multi;
        next.field_22763 = multi;
    }

    private void pick(int slot) {
        int idx = page * perPage + slot;
        if (idx < HudStyle.ALL.size()) AuctionState.styleIndex = idx;
    }

    // no @Override on purpose (same trick as the item picker): works whichever exact signature this version uses
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pages > 1) {
            if (verticalAmount > 0) flip(-1);
            else if (verticalAmount < 0) flip(1);
        }
        return true;
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        int cx = this.field_22789 / 2;

        Gfx.centered(ctx, this.field_22793, "Choose a Style", cx, 10, 0xFFFFFFFF, true);
        String sub = "Click a style to use it" + (pages > 1 ? "   (page " + (page + 1) + "/" + pages + ")" : "");
        Gfx.centered(ctx, this.field_22793, sub, cx, 22, 0xFFA0A0A0, false);

        var m = ctx.method_51448();
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
            AuctionHud.drawPreview(ctx, this.field_22793, st);
            m.popMatrix();

            if (selected) {
                Gfx.border(ctx, x - 2, y - 2, cardW + 4, cardH + 4, 0xFFFFFFFF);
                Gfx.border(ctx, x - 1, y - 1, cardW + 2, cardH + 2, 0xFFFFFFFF);
            } else if (hover) {
                Gfx.border(ctx, x - 1, y - 1, cardW + 2, cardH + 2, 0xB0FFFFFF);
            }

            String label = (selected ? "\u2713 " : "") + st.name;
            Gfx.centered(ctx, this.field_22793, label, x + cardW / 2, y + cardH + 3,
                    selected ? 0xFFFFFFFF : (hover ? 0xFFE0E0E0 : 0xFFA0A0A0), selected);
        }
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(parent);
    }
}
