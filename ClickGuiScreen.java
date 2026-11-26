package dev.actest;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClickGuiScreen extends Screen {
    private static final int W = 160, ROW = 16, SROW = 14;
    private static final Set<Module> EXPANDED = new HashSet<>();

    private enum Kind { HEADER, MODULE, TOGGLE, NUMBER }

    private record Row(Kind kind, Module module, String key, int x, int y, int w, int h) {}

    private final List<Row> rows = new ArrayList<>();

    public ClickGuiScreen() {
        super(Text.literal("AC Test"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        buildRows();
        for (Row r : rows) draw(ctx, r);
        int bottom = rows.get(rows.size() - 1).y() + rows.get(rows.size() - 1).h();
        ctx.drawTextWithShadow(textRenderer, "Left click: toggle  |  Right click: settings  |  RShift: close",
            20, bottom + 6, 0xFFAAAAAA);
    }

    private void buildRows() {
        rows.clear();
        int x = 20, y = 20;
        rows.add(new Row(Kind.HEADER, null, null, x, y, W, ROW));
        y += ROW;
        for (Module m : ACTestClient.MANAGER.modules) {
            rows.add(new Row(Kind.MODULE, m, null, x, y, W, ROW));
            y += ROW;
            if (EXPANDED.contains(m)) {
                for (String key : m.settings.keySet()) {
                    Kind k = m.toggles.contains(key) ? Kind.TOGGLE : Kind.NUMBER;
                    rows.add(new Row(k, m, key, x, y, W, SROW));
                    y += SROW;
                }
            }
        }
    }

    private void draw(DrawContext ctx, Row r) {
        int x = r.x(), y = r.y(), w = r.w(), h = r.h();
        if (r.kind() == Kind.HEADER) {
            ctx.fill(x, y, x + w, y + h, 0xFF8A2BE2);
            ctx.drawCenteredTextWithShadow(textRenderer, "AC TEST", x + w / 2, y + 4, 0xFFFFFFFF);
        } else if (r.kind() == Kind.MODULE) {
            boolean on = r.module().isEnabled();
            ctx.fill(x, y, x + w, y + h, on ? 0xFF7B2FBE : 0xE0151515);
            ctx.drawTextWithShadow(textRenderer, r.module().name, x + 6, y + 4, 0xFFFFFFFF);
            String arrow = EXPANDED.contains(r.module()) ? "-" : "+";
            ctx.drawTextWithShadow(textRenderer, arrow, x + w - 12, y + 4, 0xFFCCCCCC);
        } else if (r.kind() == Kind.TOGGLE) {
            ctx.fill(x, y, x + w, y + h, 0xE0262626);
            ctx.drawTextWithShadow(textRenderer, r.key(), x + 10, y + 3, 0xFFDDDDDD);
            boolean on = r.module().get(r.key()) > 0;
            ctx.drawTextWithShadow(textRenderer, on ? "ON" : "OFF", x + w - 26, y + 3,
                on ? 0xFF66FF66 : 0xFFFF6666);
        } else {
            ctx.fill(x, y, x + w, y + h, 0xE0262626);
            ctx.drawTextWithShadow(textRenderer, r.key(), x + 10, y + 3, 0xFFDDDDDD);
            ctx.fill(x + w - 56, y + 1, x + w - 44, y + h - 1, 0xFF444444);
            ctx.fill(x + w - 14, y + 1, x + w - 2, y + h - 1, 0xFF444444);
            ctx.drawTextWithShadow(textRenderer, "-", x + w - 52, y + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "+", x + w - 10, y + 3, 0xFFFFFFFF);
            String val = fmt(r.module().get(r.key()));
            ctx.drawTextWithShadow(textRenderer, val, x + w - 42, y + 3, 0xFFFFFF88);
        }
    }

    private static String fmt(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Row r : rows) {
            if (mx < r.x() || mx >= r.x() + r.w() || my < r.y() || my >= r.y() + r.h()) continue;
            Module m = r.module();
            if (r.kind() == Kind.MODULE) {
                if (button == 0) {
                    m.toggle();
                } else if (button == 1) {
                    if (!EXPANDED.remove(m)) EXPANDED.add(m);
                }
                return true;
            } else if (r.kind() == Kind.TOGGLE) {
                m.settings.put(r.key(), m.get(r.key()) > 0 ? 0.0 : 1.0);
                return true;
            } else if (r.kind() == Kind.NUMBER) {
                double[] rg = m.ranges.getOrDefault(r.key(), new double[]{0, 100, 0.1});
                double v = m.get(r.key());
                if (mx >= r.x() + r.w() - 14) v += rg[2];
                else if (mx >= r.x() + r.w() - 56 && mx < r.x() + r.w() - 44) v -= rg[2];
                else return true;
                v = Math.max(rg[0], Math.min(rg[1], v));
                v = Math.round(v * 1000.0) / 1000.0;
                m.settings.put(r.key(), v);
                return true;
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
