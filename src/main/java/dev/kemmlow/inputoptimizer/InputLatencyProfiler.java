package dev.kemmlow.inputoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kemmlow.inputoptimizer.rawinput.RawInputManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class InputLatencyProfiler {
    private static final LatencyMeasurements DATA = new LatencyMeasurements();
    private static List<String> cached = List.of();
    private static long lastReport;
    private static boolean openConfig;
    private static int currentBindings;
    private static long currentStart = LatencyMeasurements.NO_SAMPLE;
    private static InputConstants.Key currentKey;
    private InputLatencyProfiler() { }
    public static boolean enabled() { return InputOptimizerConfig.get().latencyProfiler; }
    private static boolean gameplay() {
        Minecraft c = Minecraft.getInstance();
        return c != null && c.isSameThread() && c.player != null && c.screen == null && c.isWindowActive() && !c.isPaused();
    }
    public static long beginCallback() {
        currentBindings = 0; currentStart = LatencyMeasurements.NO_SAMPLE; currentKey = null;
        return enabled() && gameplay() ? System.nanoTime() : LatencyMeasurements.NO_SAMPLE;
    }
    public static void endCallback(long start) {
        if (enabled() && start != LatencyMeasurements.NO_SAMPLE) DATA.callbacks.add(System.nanoTime() - start);
        currentBindings = 0; currentStart = LatencyMeasurements.NO_SAMPLE; currentKey = null;
    }
    private static KeyMapping binding(Options o, int i) {
        return i == 0 ? o.keyAttack : i == 1 ? o.keyUse : o.keyHotbarSlots[i - 2];
    }
    public static void keyPress(KeyEvent input, int action, long start) {
        if (start == LatencyMeasurements.NO_SAMPLE || action != 1) return;
        Options o = Minecraft.getInstance().options;
        currentStart = start; currentKey = InputConstants.getKey(input);
        for (int i = 0; i < 11; i++) if (binding(o, i).matches(input)) currentBindings |= 1 << i;
    }
    public static void mousePress(MouseButtonInfo input, int action, long start) {
        if (start == LatencyMeasurements.NO_SAMPLE || action != 1) return;
        Options o = Minecraft.getInstance().options;
        MouseButtonEvent event = new MouseButtonEvent(0, 0, input);
        currentStart = start; currentKey = InputConstants.Type.MOUSE.getOrCreate(input.button());
        for (int i = 0; i < 11; i++) if (binding(o, i).matchesMouse(event)) currentBindings |= 1 << i;
    }
    public static void clicked(InputConstants.Key key) {
        if (!enabled() || currentBindings == 0 || !key.equals(currentKey) || currentStart == LatencyMeasurements.NO_SAMPLE) return;
        for (int i = 0; i < 11; i++) if ((currentBindings & (1 << i)) != 0) DATA.press(i, currentStart);
        currentBindings = 0;
    }
    public static void consumed(KeyMapping key) {
        if (!enabled() || !gameplay()) return;
        Options o = Minecraft.getInstance().options;
        for (int i = 0; i < 11; i++) if (binding(o, i) == key) { DATA.consumed(i, System.nanoTime()); return; }
    }
    public static void action(int binding) { if (enabled() && gameplay()) DATA.action(binding, System.nanoTime()); }
    public static void endDispatch() { if (enabled()) DATA.clearActions(); }
    public static void rawQueue(long timestamp) { if (enabled() && gameplay()) DATA.rawQueue.add(System.nanoTime() - timestamp); }
    public static void tick() {
        if (openConfig) {
            openConfig = false;
            Minecraft.getInstance().setScreen(new InputOptimizerConfigScreen(null));
        }
        if (enabled() && !gameplay()) DATA.clearPending();
    }
    public static void reset() { DATA.reset(); cached = List.of(); lastReport = 0; }
    public static List<String> report() {
        long now = System.nanoTime();
        if (cached.isEmpty() || now - lastReport >= 500_000_000L) {
            cached = List.of(
                    "Active mode: " + RawInputManager.modeDescription(),
                    "Profiler: " + (enabled() ? "Yes" : "No") + " | last 4096 samples per measurement",
                    "Raw backend: " + (RawInputManager.isActive() ? RawInputManager.getEngine().platformName() : "inactive"),
                    DATA.callbacks.summary("Callback handling"),
                    DATA.keybinds.summary("Callback -> keybind"),
                    DATA.actionStarts.summary("Callback -> action start"),
                    DATA.rawQueue.summary("Raw read -> replay"),
                    "Keybind/action samples: fresh attack, use and hotbar presses.",
                    "Action start = client attempt; server acceptance is not measured.",
                    "Raw read -> replay excludes time before the OS data was read.",
                    "Different raw/normal sources are not a total-latency comparison.",
                    "Compare at the same FPS, scene and input rate after restarting.",
                    "These timings do not measure physical click -> visible response.");
            lastReport = now;
        }
        return cached;
    }
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
            dispatcher.register(ClientCommandManager.literal("inputlatency")
                    .executes(context -> { for (String line : report()) context.getSource().sendFeedback(Component.literal(line)); return 1; })
                    .then(ClientCommandManager.literal("on").executes(context -> { setEnabled(true); context.getSource().sendFeedback(Component.literal("Input latency profiling: Yes")); return 1; }))
                    .then(ClientCommandManager.literal("off").executes(context -> { setEnabled(false); context.getSource().sendFeedback(Component.literal("Input latency profiling: No")); return 1; }))
                    .then(ClientCommandManager.literal("save").executes(context -> { context.getSource().sendFeedback(Component.literal(saveReport())); return 1; }))
                    .then(ClientCommandManager.literal("reset").executes(context -> { reset(); context.getSource().sendFeedback(Component.literal("Input latency samples reset.")); return 1; })));
            dispatcher.register(ClientCommandManager.literal("inputoptimizer").executes(context -> {
                // Open next tick, after the command chat screen has closed.
                openConfig = true; return 1;
            }));
        });
    }
    private static void setEnabled(boolean value) { InputOptimizerConfig.get().latencyProfiler = value; InputOptimizerConfig.save(); reset(); }
    public static String saveReport() {
        try {
            Path directory = FabricLoader.getInstance().getGameDir().resolve("inputoptimizer-reports");
            Files.createDirectories(directory);
            Path file = directory.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + ".txt");
            cached = List.of();
            List<String> lines = new java.util.ArrayList<>(report());
            lines.add("FPS when saved: " + Minecraft.getInstance().getFps());
            Files.write(file, lines);
            return "Saved in inputoptimizer-reports/" + file.getFileName();
        } catch (Exception failure) {
            Main.LOGGER.warn("Cannot save input latency report", failure);
            return "Could not save report; see latest.log.";
        }
    }
}
