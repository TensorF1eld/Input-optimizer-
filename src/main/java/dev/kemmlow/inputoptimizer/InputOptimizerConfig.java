package dev.kemmlow.inputoptimizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InputOptimizerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static InputOptimizerConfig settings = new InputOptimizerConfig();
    public boolean enabled = true;
    public boolean rawKeyboard = true;
    public boolean rawMouse = true;
    public boolean immediateKeybinds = true;
    public boolean latencyProfiler = false;

    public static InputOptimizerConfig get() { return settings; }
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("inputoptimizer.json"); }
    public static void load() {
        if (Files.exists(path())) {
            try (Reader reader = Files.newBufferedReader(path())) {
                InputOptimizerConfig loaded = GSON.fromJson(reader, InputOptimizerConfig.class);
                if (loaded != null) settings = loaded;
            } catch (Exception error) { Main.LOGGER.warn("Cannot read Input Optimizer settings; using defaults", error); }
        }
        save();
    }
    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer writer = Files.newBufferedWriter(path())) { GSON.toJson(settings, writer); }
        } catch (Exception error) { Main.LOGGER.warn("Cannot save Input Optimizer settings", error); }
    }
}
