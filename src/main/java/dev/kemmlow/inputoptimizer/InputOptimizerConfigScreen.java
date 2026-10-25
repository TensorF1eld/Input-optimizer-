package dev.kemmlow.inputoptimizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class InputOptimizerConfigScreen extends Screen {
    private final Screen parent;
    public InputOptimizerConfigScreen(Screen parent) { super(Component.literal("Input Optimizer")); this.parent = parent; }
    @Override protected void init() {
        InputOptimizerConfig config = InputOptimizerConfig.get();
        int y = Math.max(24, (height - 160) / 2);
        option(y, "Optimization", config.enabled, value -> config.enabled = value, true,
                "No uses normal Minecraft input. Restart to compare modes.");
        option(y + 20, "Raw keyboard", config.rawKeyboard, value -> config.rawKeyboard = value, true,
                "Read keyboard events directly. No keeps normal keyboard callbacks.");
        option(y + 40, "Raw mouse", config.rawMouse, value -> config.rawMouse = value, true,
                "Replay mouse movement, buttons and scrolling in arrival order.");
        option(y + 60, "Immediate keybinds", config.immediateKeybinds, value -> config.immediateKeybinds = value, true,
                "Process fresh gameplay inputs immediately. Chat and held repeats keep normal timing.");
        option(y + 80, "Latency profiler", config.latencyProfiler, value -> {
            config.latencyProfiler = value;
            InputLatencyProfiler.reset();
        }, false, "Measure input handling locally. Use Latency results for statistics.");
        int buttonWidth = Math.min(300, width - 40);
        addRenderableWidget(Button.builder(Component.literal("Latency results"), b ->
                minecraft.setScreen(new InputLatencyScreen(this)))
                .bounds(width / 2 - buttonWidth / 2, y + 108, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - buttonWidth / 2, y + 132, buttonWidth, 20).build());
    }
    private void option(int y, String name, boolean value, Consumer<Boolean> setter, boolean restart, String tip) {
        int w = Math.min(300, width - 40);
        addRenderableWidget(CycleButton.booleanBuilder(Component.translatable("gui.yes"), Component.translatable("gui.no"), value)
                .withTooltip(ignored -> Tooltip.create(Component.literal(tip + (restart ? " Restart Minecraft to apply." : ""))))
                .create(width / 2 - w / 2, y, w, 20, Component.literal(name), (button, selected) -> {
                    setter.accept(selected); InputOptimizerConfig.save();
                }));
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        if (height >= 220) graphics.drawCenteredString(font, Component.literal("Input mode changes require a restart."), width / 2, height - 12, 0xFFAAAAAA);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
