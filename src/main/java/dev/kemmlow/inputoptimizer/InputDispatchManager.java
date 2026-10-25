package dev.kemmlow.inputoptimizer;

import dev.kemmlow.inputoptimizer.mixin.MixinMinecraftInvoker;
import net.minecraft.client.Minecraft;

public final class InputDispatchManager {
    private static boolean dispatching;

    private InputDispatchManager() {
    }

    public static void dispatch() {
        if (!dev.kemmlow.inputoptimizer.rawinput.RawInputManager.immediateKeybindsEnabled()) return;
        if (dispatching) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        if (!client.isSameThread()) {
            return;
        }
        if (client.screen != null) {
            return;
        }
        if (client.isPaused()) {
            return;
        }
        if (client.level == null || client.player == null) {
            return;
        }
        if (client.gameMode == null) {
            return;
        }

        // Process keybind inputs immediately. Leave network flushing to Minecraft
        // and the client's packet scheduler so tick alignment remains in control.
        dispatching = true;
        try {
            ((MixinMinecraftInvoker)client).invokeHandleKeybinds();
        } finally {
            dispatching = false;
        }
    }

    public static boolean isDispatching() {
        return dispatching;
    }
}
