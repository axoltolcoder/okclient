package dev.actest;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class Module {
    public final String name;
    private boolean enabled;
    public final Map<String, Double> settings = new LinkedHashMap<>();
    /** key -> {min, max, step} */
    public final Map<String, double[]> ranges = new HashMap<>();
    /** setting keys that are on/off switches (stored as 0 or 1) */
    public final Set<String> toggles = new HashSet<>();

    protected Module(String name) {
        this.name = name;
    }

    protected void setting(String key, double def, double min, double max, double step) {
        settings.put(key, def);
        ranges.put(key, new double[]{min, max, step});
    }

    protected void toggle(String key, boolean def) {
        settings.put(key, def ? 1.0 : 0.0);
        toggles.add(key);
    }

    public double get(String key) {
        return settings.getOrDefault(key, 0.0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}

    public abstract void onTick(MinecraftClient mc);
}
