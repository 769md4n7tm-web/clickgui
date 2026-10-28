package com.example.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClickGuiScreen extends Screen {
    private static final long LONG_PRESS_MS = 400;
    private static final int PANEL_BG = 0xCC0B1A3A;

    private static List<Panel> saved; // keeps panel positions between openings

    private Panel dragging;
    private double dragDX, dragDY, startX, startY;
    private boolean dragMoved;

    private Region pressed;
    private Panel pressedPanel;
    private long pressTime;
    private boolean longFired;

    private Btn rainbowBtn = new Btn(0, 0, 0, 0), touchBtn = new Btn(0, 0, 0, 0);

    public ClickGuiScreen() { super(Text.literal("ClickGUI")); }

    record Region(int x, int y, int w, int h, Module module, BoolSetting setting) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    record Btn(int x, int y, int w, int h) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    static int rowH() { return ClickGuiMod.touchMode ? 22 : 15; }
    static int panelW() { return ClickGuiMod.touchMode ? 122 : 108; }

    @Override
    protected void init() {
        if (saved != null) return;
        saved = new ArrayList<>();
        int pw = panelW(), x = 8, y = 8;
        for (Category c : Category.values()) {
            if (x > 8 && x + pw > width) { x = 8; y += rowH() + 12; }
            saved.add(new Panel(c, x, y, !ClickGuiMod.touchMode)); // touch: start collapsed
            x += pw + 6;
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta); // vanilla blur + dim background (call only once)

        if (pressed != null && !longFired && System.currentTimeMillis() - pressTime > LONG_PRESS_MS) {
            toggleExpand(pressedPanel, pressed.module()); // long-press = right-click
            longFired = true;
        }

        for (Panel p : saved) p.render(ctx, mx, my);

        int bh = rowH(), bw = 84, by = height - bh - 6;
        rainbowBtn = new Btn(width / 2 - bw - 4, by, bw, bh);
        touchBtn = new Btn(width / 2 + 4, by, bw, bh);
        drawToggle(ctx, rainbowBtn, "Rainbow: " + (Rainbow.enabled ? "ON" : "OFF"));
        drawToggle(ctx, touchBtn, "Touch: " + (ClickGuiMod.touchMode ? "ON" : "OFF"));
    }

    private void drawToggle(DrawContext ctx, Btn b, String label) {
        roundRect(ctx, b.x(), b.y(), b.w(), b.h(), PANEL_BG);
        outline(ctx, b.x(), b.y(), b.w(), b.h());
        ctx.drawCenteredTextWithShadow(textRenderer, label, b.x() + b.w() / 2, b.y() + (b.h() - 8) / 2, 0xFFFFFFFF);
    }

    private void toggleExpand(Panel p, Module m) {
        if (!p.expanded.remove(m)) p.expanded.add(m);
    }

    private void front(Panel p) {
        saved.remove(p);
        saved.add(p);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();

        if (b == 0 && rainbowBtn.contains(mx, my)) { Rainbow.enabled = !Rainbow.enabled; return true; }
        if (b == 0 && touchBtn.contains(mx, my)) { ClickGuiMod.touchMode = !ClickGuiMod.touchMode; saved = null; init(); return true; }

        for (int i = saved.size() - 1; i >= 0; i--) {
            Panel p = saved.get(i);
            if (p.headerHit(mx, my)) {
                front(p);
                dragging = p;
                dragDX = mx - p.x; dragDY = my - p.y;
                startX = mx; startY = my;
                dragMoved = false;
                return true;
            }
            if (!p.open) continue;
            for (Region r : p.regions) {
                if (!r.contains(mx, my)) continue;
                front(p);
                if (r.setting() != null) {
                    if (b == 0) r.setting().value = !r.setting().value;
                } else if (b == 1) {
                    toggleExpand(p, r.module());          // desktop right-click
                } else if (b == 0) {
                    pressed = r; pressedPanel = p;          // tap = toggle on release, hold = expand
                    pressTime = System.currentTimeMillis();
                    startX = mx; startY = my;
                    longFired = false;
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mx = click.x(), my = click.y();
        if (dragging != null) {
            if (Math.hypot(mx - startX, my - startY) > 4) dragMoved = true;
            if (dragMoved) {
                dragging.x = (int) Math.max(0, Math.min(width - dragging.w, mx - dragDX));
                dragging.y = (int) Math.max(0, Math.min(height - dragging.headerH, my - dragDY));
            }
            return true;
        }
        if (pressed != null && Math.hypot(mx - startX, my - startY) > 8) pressed = null; // finger moved: cancel
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            if (!dragMoved) dragging.open = !dragging.open; // tap header = collapse/expand
            dragging = null;
            return true;
        }
        if (pressed != null) {
            if (!longFired) pressed.module().toggle();
            pressed = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    // ---------- drawing helpers ----------

    static void roundRect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 2, y, x + w - 2, y + h, col);
        c.fill(x + 1, y + 1, x + 2, y + h - 1, col);
        c.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, col);
        c.fill(x, y + 2, x + 1, y + h - 2, col);
        c.fill(x + w - 1, y + 2, x + w, y + h - 2, col);
    }

    static void outline(DrawContext c, int x, int y, int w, int h) {
        int top = Rainbow.color(y * 0.004f), bot = Rainbow.color((y + h) * 0.004f);
        c.fill(x + 2, y, x + w - 2, y + 1, top);
        c.fill(x + 2, y + h - 1, x + w - 2, y + h, bot);
        c.fillGradient(x, y + 2, x + 1, y + h - 2, top, bot);
        c.fillGradient(x + w - 1, y + 2, x + w, y + h - 2, top, bot);
        c.fill(x + 1, y + 1, x + 2, y + 2, top);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, top);
        c.fill(x + 1, y + h - 2, x + 2, y + h - 1, bot);
        c.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, bot);
    }

    // ---------- panel ----------

    static final class Panel {
        final Category cat;
        final List<Module> mods;
        final Set<Module> expanded = new HashSet<>();
        final List<Region> regions = new ArrayList<>();
        int x, y, w = panelW(), headerH = rowH() + 4;
        boolean open;

        Panel(Category cat, int x, int y, boolean open) {
            this.cat = cat; this.x = x; this.y = y; this.open = open;
            this.mods = ModuleManager.in(cat);
        }

        boolean headerHit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + headerH;
        }

        void render(DrawContext ctx, int mx, int my) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int rh = rowH();
            w = panelW();
            headerH = rh + 4;
            regions.clear();

            int h = headerH;
            if (open) for (Module m : mods) {
                h += rh;
                if (expanded.contains(m)) h += m.settings.size() * rh;
            }

            roundRect(ctx, x, y, w, h, PANEL_BG);

            // header
            int c1 = Rainbow.color(y * 0.004f), c2 = Rainbow.color((y + headerH) * 0.004f);
            ctx.fillGradient(x + 2, y + 1, x + w - 2, y + headerH, Rainbow.alpha(c1, 0x99), Rainbow.alpha(c2, 0x55));
            ctx.drawTextWithShadow(tr, cat.label, x + 7, y + (headerH - 8) / 2, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, open ? "v" : ">", x + w - 11, y + (headerH - 8) / 2, 0xFFFFFFFF);

            int cy = y + headerH;
            if (open) {
                for (Module m : mods) {
                    boolean hov = mx >= x && mx < x + w && my >= cy && my < cy + rh;
                    if (m.isEnabled()) {
                        ctx.fillGradient(x + 2, cy, x + w - 2, cy + rh,
                                Rainbow.color(cy * 0.004f), Rainbow.color((cy + rh) * 0.004f));
                    } else if (hov) {
                        ctx.fill(x + 2, cy, x + w - 2, cy + rh, 0x33FFFFFF);
                    }
                    int ty = cy + (rh - 8) / 2;
                    ctx.drawTextWithShadow(tr, m.name, x + 7, ty, m.isEnabled() ? 0xFFFFFFFF : 0xFFC8CCD8);
                    ctx.drawTextWithShadow(tr, expanded.contains(m) ? "v" : ">", x + w - 11, ty, 0xFFAAAAAA);
                    regions.add(new Region(x, cy, w, rh, m, null));
                    cy += rh;

                    if (expanded.contains(m)) {
                        for (BoolSetting s : m.settings) {
                            ctx.fill(x + 2, cy, x + w - 2, cy + rh, 0x55000000);
                            int col = s.value ? Rainbow.color(cy * 0.004f) : 0xFF8890A0;
                            ctx.drawTextWithShadow(tr, s.name + (s.value ? "  ON" : "  OFF"), x + 12, cy + (rh - 8) / 2, col);
                            regions.add(new Region(x, cy, w, rh, m, s));
                            cy += rh;
                        }
                    }
                }
            }

            outline(ctx, x, y, w, h);
        }
    }
}
