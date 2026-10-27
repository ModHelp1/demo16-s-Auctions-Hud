package com.demo16.auctionhud;

import java.util.ArrayList;
import java.util.List;

/** A colour theme for the auction HUD (and the winner screen). */
public final class HudStyle {
    public static final List<HudStyle> ALL = new ArrayList<>();

    public final String name;
    public final int bg, border, accent, dim, boxBg, boxBorder, bar, who, money;
    public int barBg, pillBg, glow;
    public boolean shadow = true;

    private HudStyle(String name, int bg, int border, int accent, int dim, int boxBg, int boxBorder, int bar, int who, int money) {
        this.name = name;
        this.bg = bg;
        this.border = border;
        this.accent = accent;
        this.dim = dim;
        this.boxBg = boxBg;
        this.boxBorder = boxBorder;
        this.bar = bar;
        this.who = who;
        this.money = money;
        int solid = Gfx.opaque(bg);
        this.barBg = Gfx.blend(solid, border, 0.18f);
        this.pillBg = Gfx.blend(solid, border, 0.25f);
        this.glow = border;
    }

    private HudStyle noShadow() { this.shadow = false; return this; }
    private HudStyle glow(int c) { this.glow = c; return this; }
    private HudStyle pillBg(int c) { this.pillBg = c; return this; }
    private HudStyle barBg(int c) { this.barBg = c; return this; }

    private static void add(HudStyle s) { ALL.add(s); }

    static {
        //                 name              bg          border      accent      dim         boxBg       boxBorder   bar         who         money
        add(new HudStyle("Midnight",        0xF0101018, 0xFFE8E8F0, 0xFFFFFFFF, 0xFF9A9AB0, 0xFF14141E, 0xFFE8E8F0, 0xFFE8E8F0, 0xFFFFFFFF, 0xFFFFFFFF));
        add(new HudStyle("Graphite",        0xF01C1C1C, 0xFF7A7A7A, 0xFFE0E0E0, 0xFF8A8A8A, 0xFF262626, 0xFF5A5A5A, 0xFFA0A0A0, 0xFFF0F0F0, 0xFFFFFFFF));
        add(new HudStyle("Paper",           0xF0F2F0EA, 0xFF3A3A3A, 0xFF1E1E1E, 0xFF5A5A5A, 0xFFFFFFFF, 0xFF1E1E1E, 0xFF1E1E1E, 0xFF111111, 0xFF111111)
                .noShadow().glow(0xFFE0A800).pillBg(0xFFDDDDD5).barBg(0xFFCFCFC8));
        add(new HudStyle("Classic Orange",  0xF01A1006, 0xFFFF9A1F, 0xFFFFB347, 0xFFB07A2A, 0xFF2A1A05, 0xFFFF9A1F, 0xFFFF9A1F, 0xFFFFE0B0, 0xFFFFC266));
        add(new HudStyle("Emerald",         0xF0071A0E, 0xFF2EE66B, 0xFF6BFF9A, 0xFF2E9A55, 0xFF0B2A14, 0xFF2EE66B, 0xFF2EE66B, 0xFFD6FFE2, 0xFF6BFF9A));
        add(new HudStyle("Diamond",         0xF0061A1C, 0xFF4DE8E0, 0xFF8FFFF6, 0xFF2E9A9A, 0xFF0A2A2C, 0xFF4DE8E0, 0xFF4DE8E0, 0xFFE0FFFC, 0xFF8FFFF6));
        add(new HudStyle("Gold Rush",       0xE6120F00, 0xFFFFD21F, 0xFFFFE066, 0xFFB89B2A, 0xFF1E1900, 0xFFFFD21F, 0xFFFFD21F, 0xFFFFF4C2, 0xFFFFE066));
        add(new HudStyle("Redstone",        0xF01A0606, 0xFFFF3B3B, 0xFFFF7A7A, 0xFF9A3A3A, 0xFF2A0A0A, 0xFFFF3B3B, 0xFFFF3B3B, 0xFFFFD6D6, 0xFFFF7A7A));
        add(new HudStyle("Nether",          0xF02A0E04, 0xFFFF6A00, 0xFFFF8C3A, 0xFFB0602A, 0xFF3A1606, 0xFFFF6A00, 0xFFFF6A00, 0xFFFFD2B0, 0xFFFF8C3A));
        add(new HudStyle("Ender",           0xF0100818, 0xFF3DE8D0, 0xFFC78BFF, 0xFF7A5AA8, 0xFF1A0E2A, 0xFF3DE8D0, 0xFF3DE8D0, 0xFFF0E0FF, 0xFFC78BFF));
        add(new HudStyle("Prismarine",      0xF00A1C18, 0xFF4FB59A, 0xFF8FE8D0, 0xFF3E8A78, 0xFF0E2A24, 0xFF4FB59A, 0xFF4FB59A, 0xFFDDFFF6, 0xFF8FE8D0));
        add(new HudStyle("Amethyst",        0xF0160A22, 0xFFB57BFF, 0xFFD2A6FF, 0xFF8A5ACC, 0xFF241036, 0xFFB57BFF, 0xFFB57BFF, 0xFFF0E0FF, 0xFFD2A6FF));
        add(new HudStyle("Cherry Blossom",  0xF02A1620, 0xFFFFA6C9, 0xFFFFC4DC, 0xFFB8708C, 0xFF3A1E2C, 0xFFFFA6C9, 0xFFFFA6C9, 0xFFFFE6F0, 0xFFFFC4DC));
        add(new HudStyle("Glass",           0x90101010, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFC8C8C8, 0x66000000, 0xB0FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF));
        // ---- 5 extra styles ----
        add(new HudStyle("Frost",           0xF008141E, 0xFF9AD8FF, 0xFFD6F0FF, 0xFF6A9AB8, 0xFF0E2232, 0xFF9AD8FF, 0xFF9AD8FF, 0xFFF0FAFF, 0xFFD6F0FF));
        add(new HudStyle("Lava",            0xF0200600, 0xFFFF3A00, 0xFFFFD21F, 0xFFB0481E, 0xFF340C00, 0xFFFF8A00, 0xFFFF5A00, 0xFFFFF0C0, 0xFFFFD21F));
        add(new HudStyle("Neon",            0xF00A0A14, 0xFFFF2EC8, 0xFF2EF0FF, 0xFF8A4AA8, 0xFF140A24, 0xFF2EF0FF, 0xFFFF2EC8, 0xFFFFFFFF, 0xFFFF7AE0));
        add(new HudStyle("Royal",           0xF00A1030, 0xFFFFD21F, 0xFFFFE066, 0xFF7A8AC8, 0xFF121C4A, 0xFF4A64E8, 0xFF4A64E8, 0xFFFFFFFF, 0xFFFFE066));
        add(new HudStyle("Sunset",          0xF0220A24, 0xFFFF6A8A, 0xFFFFB86A, 0xFFB06A9A, 0xFF3A1236, 0xFFFFB86A, 0xFFFF6A8A, 0xFFFFE8D0, 0xFFFFC27A));
    }

    public static int defaultIndex() {
        for (int i = 0; i < ALL.size(); i++) if (ALL.get(i).name.equals("Gold Rush")) return i;
        return 0;
    }

    public static HudStyle current() {
        int i = AuctionState.styleIndex;
        if (i < 0 || i >= ALL.size()) i = defaultIndex();
        return ALL.get(i);
    }
}
