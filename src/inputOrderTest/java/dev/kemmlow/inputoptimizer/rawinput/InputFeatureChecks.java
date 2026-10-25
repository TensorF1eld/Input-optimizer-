package dev.kemmlow.inputoptimizer.rawinput;

import com.sun.jna.Memory;
import dev.kemmlow.inputoptimizer.InputOptimizerConfig;
import dev.kemmlow.inputoptimizer.LatencyMeasurements;
import dev.kemmlow.inputoptimizer.rawinput.platform.WindowsInputDecoder;
import java.util.ArrayList;
import java.util.List;

/** Decode real native layouts, replay production ordering, and exercise bounded timing storage. */
public final class InputFeatureChecks {
    private static int checks;
    private static final int HEADER = WindowsInputDecoder.HEADER_SIZE;
    public static void main(String[] args) {
        keyboardPressRelease(); modifiersAndRepeat(); extendedAndKeypad(); scrollLayout();
        mouseMovementButtonPacket(); movementClickMovement(); movementBatchAndTrailing();
        disabledSources(); modeControls(); timingCorrelation(); staleAndBoundedTimings(); focusClearsMovement();
        offsetPacket(); independentPressTimings();
        System.out.println("Input feature checks passed: " + checks);
    }
    private static void keyboardPressRelease() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        for (int flags : new int[]{0, 1}) key(decoder, 114, 0x3d, flags, events);
        equal(events.size(), 2);
        RawKeyEvent press = (RawKeyEvent) events.get(0), release = (RawKeyEvent) events.get(1);
        equal(press.key, 292); equal(press.action, 1); equal(release.action, 0); equal(press.scancode, 0x3d);
        checks++;
    }
    private static void modifiersAndRepeat() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        key(decoder, 16, 0x36, 0, events); key(decoder, 65, 0x1e, 0, events);
        key(decoder, 65, 0x1e, 0, events); key(decoder, 16, 0x36, 1, events);
        equal(((RawKeyEvent) events.get(0)).key, 344);
        equal(((RawKeyEvent) events.get(1)).modifiers, 1);
        equal(((RawKeyEvent) events.get(2)).action, 2);
        equal(((RawKeyEvent) events.get(3)).modifiers, 0);
        decoder.resetHeld(); events.clear(); key(decoder, 65, 0x1e, 0, events);
        equal(((RawKeyEvent) events.get(0)).action, 1); checks++;
    }
    private static void extendedAndKeypad() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        key(decoder, 17, 0x1d, 2, events); key(decoder, 17, 0x1d, 3, events);
        key(decoder, 13, 0x1c, 2, events); key(decoder, 45, 0x52, 0, events);
        key(decoder, 19, 0x45, 4, events);
        equal(((RawKeyEvent) events.get(0)).key, 345); equal(((RawKeyEvent) events.get(0)).scancode, 0x11d);
        equal(((RawKeyEvent) events.get(1)).action, 0); equal(((RawKeyEvent) events.get(2)).key, 335);
        equal(((RawKeyEvent) events.get(3)).key, 320); equal(((RawKeyEvent) events.get(4)).key, 284);
        checks++;
    }
    private static void scrollLayout() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        try (Memory buffer = mouseBuffer()) {
            buffer.setShort(HEADER + 4L, (short) 0x400); buffer.setShort(HEADER + 6L, (short) -120);
            buffer.setInt(HEADER + 8L, 999); // ulRawButtons must not be read as wheel data.
            decoder.decode(buffer, 0, HEADER + 24, 50, true, true, true, events::add);
            RawScrollEvent vertical = (RawScrollEvent) events.remove(0); equal(vertical.vertical, -1.0);
            buffer.setShort(HEADER + 4L, (short) 0x800); buffer.setShort(HEADER + 6L, (short) 60);
            decoder.decode(buffer, 0, HEADER + 24, 51, true, true, true, events::add);
            RawScrollEvent horizontal = (RawScrollEvent) events.get(0); equal(horizontal.horizontal, 0.5);
        }
        checks++;
    }
    private static void mouseMovementButtonPacket() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        try (Memory buffer = mouseBuffer()) {
            buffer.setInt(HEADER + 12L, 7); buffer.setInt(HEADER + 16L, -2); buffer.setShort(HEADER + 4L, (short) 1);
            decoder.decode(buffer, 0, HEADER + 24, 50, true, true, true, events::add);
            equal(events.get(0), new RawMoveEvent(7, -2, 50));
            equal(((RawButtonEvent) events.get(1)).action, 1);
            events.clear(); decoder.decode(buffer, 0, HEADER + 24, 50, true, true, false, events::add);
            equal(events.size(), 1); truth(events.get(0) instanceof RawButtonEvent);
        }
        checks++;
    }
    private static void movementClickMovement() {
        Engine engine = new Engine(); engine.add(new RawMoveEvent(10, 0, 1));
        engine.add(new RawButtonEvent(0, 1, 0, 2)); engine.add(new RawMoveEvent(-10, 0, 3));
        double[] position = {0}; List<Double> clicks = new ArrayList<>();
        RawInputReplay.drain(engine, new ReplaySuppression(), event -> {
            if (event instanceof RawMoveEvent move) position[0] += move.dx();
        }, () -> clicks.add(position[0]));
        equal(clicks, List.of(10.0)); equal(position[0], 0.0); checks++;
    }
    private static void movementBatchAndTrailing() {
        Engine engine = new Engine(); engine.add(new RawMoveEvent(2, 1, 10)); engine.add(new RawMoveEvent(3, -1, 11));
        engine.add(new RawButtonEvent(0, 1, 0, 12)); engine.add(new RawMoveEvent(4, 0, 13));
        List<RawInputEvent> replayed = new ArrayList<>(); int[] dispatches = {0};
        RawInputReplay.drain(engine, new ReplaySuppression(), replayed::add, () -> dispatches[0]++);
        equal(replayed.get(0), new RawMoveEvent(5, 0, 10)); equal(replayed.get(2), new RawMoveEvent(4, 0, 13));
        equal(dispatches[0], 1); checks++;
    }
    private static void disabledSources() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> events = new ArrayList<>();
        try (Memory buffer = mouseBuffer()) {
            buffer.setShort(HEADER + 4L, (short) 1);
            decoder.decode(buffer, 0, HEADER + 24, 0, true, false, true, events::add); equal(events.size(), 0);
        }
        try (Memory buffer = keyBuffer(65, 30, 0)) {
            decoder.decode(buffer, 0, HEADER + 16, 0, false, true, true, events::add); equal(events.size(), 0);
        }
        checks++;
    }
    private static void modeControls() {
        InputOptimizerConfig config = new InputOptimizerConfig(); config.enabled = false;
        RawInputManager.configure(config); truth(!RawInputManager.immediateKeybindsEnabled());
        equal(RawInputManager.modeDescription(), "Normal Minecraft input");
        config.enabled = true; config.immediateKeybinds = false;
        RawInputManager.configure(config); truth(!RawInputManager.immediateKeybindsEnabled());
        config.immediateKeybinds = true; RawInputManager.configure(config); truth(RawInputManager.immediateKeybindsEnabled());
        config.immediateKeybinds = false; truth(RawInputManager.immediateKeybindsEnabled()); // requires restart/configure
        checks++;
    }
    private static void timingCorrelation() {
        LatencyMeasurements data = new LatencyMeasurements();
        data.press(0, 100); data.consumed(0, 140); data.action(0, 150); data.action(0, 160);
        equal(data.keybinds.percentile(50), 40L); equal(data.actionStarts.percentile(50), 50L);
        equal(data.actionStarts.size(), 1); data.consumed(1, 180); equal(data.keybinds.size(), 1);
        data.press(0, 200); data.consumed(0, 220); data.clearActions(); data.action(0, 250);
        equal(data.actionStarts.size(), 1); checks++;
    }
    private static void staleAndBoundedTimings() {
        LatencyMeasurements data = new LatencyMeasurements(); data.press(0, 0);
        data.consumed(0, LatencyMeasurements.MAX_PENDING_NANOS + 1); equal(data.keybinds.size(), 0);
        for (int i = 0; i < 5000; i++) data.callbacks.add(i);
        equal(data.callbacks.size(), 4096); equal(data.callbacks.percentile(100), 4999L);
        data.reset(); equal(data.callbacks.size(), 0);
        data.press(0, 20); data.clearPending(); data.consumed(0, 30); equal(data.keybinds.size(), 0); checks++;
    }
    private static void focusClearsMovement() {
        Engine engine = new Engine(); engine.setGameFocused(true); engine.queueMovement(2, 3, 1);
        engine.setFocused(false); truth(engine.pollEvent() == null); checks++;
    }
    private static void offsetPacket() {
        WindowsInputDecoder decoder = new WindowsInputDecoder(); List<RawInputEvent> output = new ArrayList<>();
        int offset = 64;
        try (Memory combined = new Memory(offset + HEADER + 16); Memory packet = keyBuffer(87, 0x11, 0)) {
            combined.clear(); combined.write(offset, packet.getByteArray(0, HEADER + 16), 0, HEADER + 16);
            decoder.decode(combined, offset, HEADER + 16, 17, true, false, true, output::add);
            equal(((RawKeyEvent) output.get(0)).key, 87); equal(output.get(0).timestamp(), 17L);
        }
        checks++;
    }
    private static void independentPressTimings() {
        LatencyMeasurements data = new LatencyMeasurements();
        data.press(0, 100); data.press(1, 200); data.press(0, 300);
        data.consumed(1, 250); data.action(1, 260);
        data.consumed(0, 350); data.action(0, 360);
        data.consumed(0, 400); data.action(0, 410);
        equal(data.keybinds.size(), 3); equal(data.keybinds.percentile(100), 250L);
        equal(data.actionStarts.size(), 3); equal(data.actionStarts.percentile(100), 260L);
        checks++;
    }
    private static void key(WindowsInputDecoder decoder, int vk, int scan, int flags, List<RawInputEvent> output) {
        try (Memory buffer = keyBuffer(vk, scan, flags)) {
            decoder.decode(buffer, 0, HEADER + 16, 1, true, true, true, output::add);
        }
    }
    private static Memory keyBuffer(int vk, int scan, int flags) {
        Memory p = new Memory(HEADER + 16); p.clear(); p.setInt(0, 1); p.setInt(4, HEADER + 16);
        p.setShort(HEADER, (short) scan); p.setShort(HEADER + 2L, (short) flags); p.setShort(HEADER + 6L, (short) vk); return p;
    }
    private static Memory mouseBuffer() { Memory p = new Memory(HEADER + 24); p.clear(); p.setInt(4, HEADER + 24); return p; }
    private static void equal(Object actual, Object expected) { if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual); }
    private static void truth(boolean value) { if (!value) throw new AssertionError("Check failed"); }
    private static final class Engine extends RawInputEngine {
        void add(RawInputEvent event) { eventQueue.add(event); }
        public boolean initialize(long window) { return true; }
        public void shutdown() { }
        public void centerCursor() { }
        public String platformName() { return "test"; }
    }
}
