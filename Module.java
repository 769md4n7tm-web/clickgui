package com.example.clickgui;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name;
    public final Category category;
    public final List<BoolSetting> settings = new ArrayList<>();
    private boolean enabled;

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
}
