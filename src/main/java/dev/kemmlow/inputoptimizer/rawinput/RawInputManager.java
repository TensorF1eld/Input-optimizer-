package dev.kemmlow.inputoptimizer.rawinput;

import com.mojang.blaze3d.platform.Window;
import dev.kemmlow.inputoptimizer.Main;
import dev.kemmlow.inputoptimizer.InputOptimizerConfig;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.platform.LinuxRawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.platform.MacOSRawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.platform.WindowsRawInputEngine;
import net.minecraft.client.Minecraft;

public final class RawInputManager {
    private static RawInputEngine engine;
    private static boolean initialized;
    private static final ReplaySuppression suppression = new ReplaySuppression();
    private static boolean optimization = true;
    private static boolean keyboard = true;
    private static boolean mouse = true;
    private static boolean immediate = true;

    public static void configure(InputOptimizerConfig config) {
        optimization = config.enabled; keyboard = config.rawKeyboard;
        mouse = config.rawMouse; immediate = config.immediateKeybinds;
    }
    public static boolean immediateKeybindsEnabled() { return optimization && immediate; }
    public static boolean isRawKeyboardActive() { return keyboard && isActive(); }
    public static boolean isRawMouseActive() { return mouse && isActive(); }
    public static boolean windowsOwnsKeyboard(int key) {
        return isRawKeyboardActive() && engine instanceof WindowsRawInputEngine
                && dev.kemmlow.inputoptimizer.rawinput.platform.WindowsInputDecoder.supportsKey(key);
    }
    public static boolean windowsOwnsMouse() { return isRawMouseActive() && engine instanceof WindowsRawInputEngine; }
    public static String modeDescription() {
        return optimization ? "Optimized (keyboard " + keyboard + ", mouse " + mouse + ", immediate " + immediate + ")" : "Normal Minecraft input";
    }

    private RawInputManager() {
    }

    public static boolean initialize(long windowHandle) {
        if (!optimization || (!keyboard && !mouse)) return false;
        if (initialized) {
            return engine != null && engine.isRunning();
        }
        initialized = true;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            engine = new WindowsRawInputEngine();
        } else if (os.contains("linux")) {
            engine = new LinuxRawInputEngine();
        } else if (os.contains("mac")) {
            engine = new MacOSRawInputEngine();
        } else {
            Main.LOGGER.warn("[Input Optimizer] Unsupported platform: {}", os);
            return false;
        }
        engine.configure(keyboard, mouse);
        boolean success = engine.initialize(windowHandle);
        if (success) {
            Main.LOGGER.info("[Input Optimizer] Raw input initialized: {}", engine.platformName());
        } else {
            Main.LOGGER.warn("[Input Optimizer] Failed to initialize raw input: {}", engine.platformName());
            engine = null;
        }
        return success;
    }

    public static boolean isActive() {
        return engine != null && engine.isRunning();
    }

    public static RawInputEngine getEngine() {
        return engine;
    }

    public static ReplaySuppression replaySuppression() {
        return suppression;
    }

    public static void markKeyConsumed(int glfwKey, int action) {
        if (windowsOwnsKeyboard(glfwKey)) return;
        suppression.markKey(glfwKey, action);
    }

    public static boolean consumeIfKeyMarked(int glfwKey, int action) {
        return suppression.consumeKey(glfwKey, action);
    }

    public static void markButtonConsumed(int button, int action) {
        if (windowsOwnsMouse()) return;
        suppression.markButton(button, action);
    }

    public static boolean consumeIfButtonMarked(int button, int action) {
        return suppression.consumeButton(button, action);
    }

    public static void markScrollConsumed(double horizontal, double vertical) {
        if (windowsOwnsMouse()) return;
        suppression.markScroll(horizontal, vertical);
    }

    public static boolean consumeIfScrollMarked(double horizontal, double vertical) {
        return suppression.consumeScroll(horizontal, vertical);
    }

    public static double[] pollBothDeltas() {
        if (engine != null) {
            return engine.pollBothDeltas();
        }
        return new double[]{0.0, 0.0};
    }

    public static void setFocused(boolean focused) {
        if (engine != null) {
            engine.setFocused(focused);
        }
        if (!focused) {
            suppression.clear();
        }
    }

    public static void setGameFocused(boolean gameFocused) {
        if (engine != null) {
            engine.setGameFocused(gameFocused);
        }
    }

    public static void syncFocusState(Minecraft client) {
        if (engine == null || client == null) {
            return;
        }
        boolean active = client.isWindowActive();
        setFocused(active);
        boolean inGame = client.screen == null;
        setGameFocused(inGame);
    }

    public static void tick() {
        if (engine == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        syncFocusState(client);
        RawInputEngine rawInputEngine = engine;
        if (rawInputEngine instanceof WindowsRawInputEngine winEngine) {
            Window window = client.getWindow();
            if (window != null) {
                winEngine.updateCenter(window.getX() + window.getScreenWidth() / 2, window.getY() + window.getScreenHeight() / 2);
            }
        }
    }

    public static void shutdown() {
        if (engine != null) {
            engine.shutdown();
            engine = null;
        }
        suppression.clear();
    }

    static {
        initialized = false;
    }
}
