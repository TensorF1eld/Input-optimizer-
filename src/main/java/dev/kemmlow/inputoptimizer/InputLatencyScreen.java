package dev.kemmlow.inputoptimizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

public final class InputLatencyScreen extends Screen {
    private final Screen parent;
    private int page;
    private String savedMessage = "";
    public InputLatencyScreen(Screen parent) { super(Component.literal("Input latency results")); this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Previous"), b -> page = Math.max(0, page - 1))
                .bounds(width / 2 - 145, height - 56, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), b -> page++)
                .bounds(width / 2 - 45, height - 56, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save report"), b -> savedMessage = InputLatencyProfiler.saveReport())
                .bounds(width / 2 + 55, height - 56, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset samples"), b -> InputLatencyProfiler.reset())
                .bounds(width / 2 - 145, height - 32, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 5, height - 32, 140, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String line : InputLatencyProfiler.report()) {
            lines.addAll(font.split(Component.literal(line), width - 32));
        }
        if (!savedMessage.isEmpty()) lines.addAll(font.split(Component.literal(savedMessage), width - 32));
        int perPage = Math.max(1, (height - 96) / (font.lineHeight + 2));
        int pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        graphics.drawCenteredString(font, Component.literal("Page " + (page + 1) + " / " + pages), width / 2, 29, 0xFFAAAAAA);
        for (int i = page * perPage, y = 43; i < Math.min(lines.size(), (page + 1) * perPage); i++, y += font.lineHeight + 2)
            graphics.drawString(font, lines.get(i), 16, y, 0xFFFFFFFF);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
