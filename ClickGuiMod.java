package com.example.clickgui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ClickGuiMod implements ClientModInitializer {
    /** Auto-on for PojavLauncher-style launchers; also toggleable in the GUI or with /clickgui touch. */
    public static boolean touchMode = System.getenv("POJAV_RENDERER") != null;

    private static boolean openRequested;
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clickgui.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("clickgui", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) openRequested = true;
            }
            if (openRequested && client.currentScreen == null && client.player != null) {
                openRequested = false;
                client.setScreen(new ClickGuiScreen());
            }
        });

        // Mobile-friendly entry points: /clickgui and a button on the pause menu.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                dispatcher.register(ClientCommandManager.literal("clickgui")
                        .executes(c -> { openRequested = true; return 1; })
                        .then(ClientCommandManager.literal("touch")
                                .executes(c -> { touchMode = !touchMode; return 1; }))));

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("ClickGUI"),
                        b -> client.setScreen(new ClickGuiScreen())).dimensions(6, 6, 80, 20).build());
            }
        });
    }
}
