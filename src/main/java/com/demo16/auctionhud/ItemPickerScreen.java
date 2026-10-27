package com.demo16.auctionhud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7923;

/** "Dropdown": search box + list of items with little icons. */
public class ItemPickerScreen extends class_437 {
    private static final int ROWS = 8;
    private static final List<class_1792> ALL = new ArrayList<>();
    private static final Map<class_1792, String> NAMES = new HashMap<>();

    private final class_437 parent;
    private class_342 search;
    private final class_4185[] rowBtns = new class_4185[ROWS];
    private List<class_1792> results = new ArrayList<>();
    private int scroll = 0;

    public ItemPickerScreen(class_437 parent) {
        super(class_2561.method_43470("Choose the item you're auctioning"));
        this.parent = parent;
    }

    private static void loadAll() {
        if (!ALL.isEmpty()) return;
        for (class_1792 item : class_7923.field_41178) {
            if (item == class_1802.field_8162) continue;
            ALL.add(item);
            NAMES.put(item, new class_1799(item).method_7964().getString());
        }
        ALL.sort(Comparator.comparing(NAMES::get));
    }

    @Override
    protected void method_25426() {
        loadAll();
        int cx = this.field_22789 / 2;

        search = new class_342(this.field_22793, cx - 100, 34, 200, 20, class_2561.method_43473());
        search.method_1880(64);
        search.method_1863(s -> { scroll = 0; refresh(); });
        this.method_37063(search);
        this.method_48265(search);

        for (int i = 0; i < ROWS; i++) {
            final int idx = i;
            rowBtns[i] = class_4185.method_46430(class_2561.method_43473(), b -> pick(idx))
                    .method_46434(cx - 100, 60 + i * 22, 200, 20).method_46431();
            this.method_37063(rowBtns[i]);
        }

        this.method_37063(class_4185.method_46430(class_2561.method_43470("\u25B2"), b -> {
            scroll = Math.max(0, scroll - ROWS);
            refresh();
        }).method_46434(cx + 104, 60, 20, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("\u25BC"), b -> {
            if (scroll + ROWS < results.size()) scroll += ROWS;
            refresh();
        }).method_46434(cx + 104, 60 + (ROWS - 1) * 22, 20, 20).method_46431());

        this.method_37063(class_4185.method_46430(class_2561.method_43470("Back"), b -> method_25419())
                .method_46434(cx - 100, 60 + ROWS * 22 + 6, 200, 20).method_46431());

        refresh();
    }

    private void refresh() {
        String q = search == null ? "" : search.method_1882().toLowerCase().trim().replace(' ', '_');
        String q2 = q.replace('_', ' ');
        results = new ArrayList<>();
        for (class_1792 item : ALL) {
            if (q.isEmpty()
                    || class_7923.field_41178.method_10221(item).method_12832().contains(q)
                    || NAMES.get(item).toLowerCase().contains(q2)) {
                results.add(item);
            }
        }
        if (scroll >= results.size()) scroll = 0;
        for (int i = 0; i < ROWS; i++) {
            int n = scroll + i;
            boolean has = n < results.size();
            rowBtns[i].field_22764 = has;
            rowBtns[i].field_22763 = has;
            if (has) rowBtns[i].method_25355(class_2561.method_43470(NAMES.get(results.get(n))));
        }
    }

    private void pick(int row) {
        int n = scroll + row;
        if (n >= results.size()) return;
        AuctionState.itemId = class_7923.field_41178.method_10221(results.get(n)).toString();
        method_25419();
    }

    // no @Override on purpose: works whichever exact signature this Minecraft version uses
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) scroll = Math.max(0, scroll - 1);
        else if (verticalAmount < 0 && scroll + ROWS < results.size()) scroll++;
        refresh();
        return true;
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        int cx = this.field_22789 / 2;
        ctx.method_27534(this.field_22793, this.field_22785, cx, 12, 0xFFFFD21F);
        ctx.method_51433(this.field_22793, "Search (e.g. totem, sword, netherite)", cx - 100, 24, 0xFFFFE066, false);
        for (int i = 0; i < ROWS; i++) {
            int n = scroll + i;
            if (n < results.size()) {
                ctx.method_51427(new class_1799(results.get(n)), cx - 100 - 22, 60 + i * 22 + 2);
            }
        }
        String info = results.size() + " items - scroll with the mouse wheel or the arrows";
        ctx.method_51433(this.field_22793, info, cx - this.field_22793.method_1727(info) / 2, 60 + ROWS * 22 + 32, 0xFFB89B2A, false);
    }

    @Override
    public void method_25419() {
        this.field_22787.method_1507(parent);
    }
}
