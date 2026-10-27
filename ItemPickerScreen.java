package com.demo16.auctionhud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;

/** "Dropdown": search box + list of items with little icons. */
public class ItemPickerScreen extends Screen {
    private static final int ROWS = 8;
    private static final List<Item> ALL = new ArrayList<>();
    private static final Map<Item, String> NAMES = new HashMap<>();

    private final Screen parent;
    private TextFieldWidget search;
    private final ButtonWidget[] rowBtns = new ButtonWidget[ROWS];
    private List<Item> results = new ArrayList<>();
    private int scroll = 0;

    public ItemPickerScreen(Screen parent) {
        super(Text.literal("Choose the item you're auctioning"));
        this.parent = parent;
    }

    private static void loadAll() {
        if (!ALL.isEmpty()) return;
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            ALL.add(item);
            NAMES.put(item, new ItemStack(item).getName().getString());
        }
        ALL.sort(Comparator.comparing(NAMES::get));
    }

    @Override
    protected void init() {
        loadAll();
        int cx = this.width / 2;

        search = new TextFieldWidget(this.textRenderer, cx - 100, 34, 200, 20, Text.empty());
        search.setMaxLength(64);
        search.setChangedListener(s -> { scroll = 0; refresh(); });
        this.addDrawableChild(search);
        this.setInitialFocus(search);

        for (int i = 0; i < ROWS; i++) {
            final int idx = i;
            rowBtns[i] = ButtonWidget.builder(Text.empty(), b -> pick(idx))
                    .dimensions(cx - 100, 60 + i * 22, 200, 20).build();
            this.addDrawableChild(rowBtns[i]);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25B2"), b -> {
            scroll = Math.max(0, scroll - ROWS);
            refresh();
        }).dimensions(cx + 104, 60, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("\u25BC"), b -> {
            if (scroll + ROWS < results.size()) scroll += ROWS;
            refresh();
        }).dimensions(cx + 104, 60 + (ROWS - 1) * 22, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(cx - 100, 60 + ROWS * 22 + 6, 200, 20).build());

        refresh();
    }

    private void refresh() {
        String q = search == null ? "" : search.getText().toLowerCase().trim().replace(' ', '_');
        String q2 = q.replace('_', ' ');
        results = new ArrayList<>();
        for (Item item : ALL) {
            if (q.isEmpty()
                    || Registries.ITEM.getId(item).getPath().contains(q)
                    || NAMES.get(item).toLowerCase().contains(q2)) {
                results.add(item);
            }
        }
        if (scroll >= results.size()) scroll = 0;
        for (int i = 0; i < ROWS; i++) {
            int n = scroll + i;
            boolean has = n < results.size();
            rowBtns[i].visible = has;
            rowBtns[i].active = has;
            if (has) rowBtns[i].setMessage(Text.literal(NAMES.get(results.get(n))));
        }
    }

    private void pick(int row) {
        int n = scroll + row;
        if (n >= results.size()) return;
        AuctionState.itemId = Registries.ITEM.getId(results.get(n)).toString();
        close();
    }

    // no @Override on purpose: works whichever exact signature this Minecraft version uses
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) scroll = Math.max(0, scroll - 1);
        else if (verticalAmount < 0 && scroll + ROWS < results.size()) scroll++;
        refresh();
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, 0xFFFFD21F);
        ctx.drawText(this.textRenderer, "Search (e.g. totem, sword, netherite)", cx - 100, 24, 0xFFFFE066, false);
        for (int i = 0; i < ROWS; i++) {
            int n = scroll + i;
            if (n < results.size()) {
                ctx.drawItem(new ItemStack(results.get(n)), cx - 100 - 22, 60 + i * 22 + 2);
            }
        }
        String info = results.size() + " items - scroll with the mouse wheel or the arrows";
        ctx.drawText(this.textRenderer, info, cx - this.textRenderer.getWidth(info) / 2, 60 + ROWS * 22 + 32, 0xFFB89B2A, false);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
