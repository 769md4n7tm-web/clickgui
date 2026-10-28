package com.example.clickgui;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        // Placeholders: replace with your own modules.
        Module sprint = new Module("Sprint", Category.MOVEMENT);
        sprint.settings.add(new BoolSetting("Omni", false));
        add(sprint);
        add(new Module("Example Combat", Category.COMBAT));
        add(new Module("Fullbright", Category.RENDER));
        add(new Module("Example Player", Category.PLAYER));
        add(new Module("Example Misc", Category.MISC));
    }

    public static void add(Module m) { MODULES.add(m); }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }
}
