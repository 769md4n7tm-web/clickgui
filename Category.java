package com.example.clickgui;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), MISC("Misc");

    public final String label;
    Category(String label) { this.label = label; }
}
