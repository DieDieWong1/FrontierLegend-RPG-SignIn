package com.example.signin.keybinding;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding startBinding;
    public static KeyBinding stopBinding;

    public static void register() {
        startBinding = new KeyBinding(
                "key.signin.start",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.signin.main"
        );
        KeyBindingHelper.registerKeyBinding(startBinding);

        stopBinding = new KeyBinding(
                "key.signin.stop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F12,
                "category.signin.main"
        );
        KeyBindingHelper.registerKeyBinding(stopBinding);
    }

    public static boolean consumeStartPressed() {
        return startBinding != null && startBinding.wasPressed();
    }

    public static boolean consumeStopPressed() {
        return stopBinding != null && stopBinding.wasPressed();
    }
}


