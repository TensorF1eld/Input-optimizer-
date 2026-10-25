/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.DoubleAdder;

public abstract class RawInputEngine {
    protected final DoubleAdder deltaX = new DoubleAdder();
    protected final DoubleAdder deltaY = new DoubleAdder();
    protected final ConcurrentLinkedQueue<RawInputEvent> eventQueue = new ConcurrentLinkedQueue<>();
    protected volatile boolean running = false;
    protected volatile boolean focused = false;
    protected volatile boolean gameFocused = false;
    protected boolean captureKeyboard = true;
    protected boolean captureMouse = true;

    public void configure(boolean keyboard, boolean mouse) {
        captureKeyboard = keyboard;
        captureMouse = mouse;
    }

    public abstract boolean initialize(long var1);

    public abstract void shutdown();

    public abstract void centerCursor();

    public abstract String platformName();

    public void accumulateDelta(double dx, double dy) {
        queueMovement(dx, dy, System.nanoTime());
    }

    public void queueMovement(double dx, double dy, long timestamp) {
        if (captureMouse && gameFocused && (dx != 0 || dy != 0)) eventQueue.add(new RawMoveEvent(dx, dy, timestamp));
    }

    public double[] pollBothDeltas() {
        return new double[]{this.deltaX.sumThenReset(), this.deltaY.sumThenReset()};
    }

    public void resetDeltas() {
        this.deltaX.reset();
        this.deltaY.reset();
    }

    public RawInputEvent pollEvent() {
        return this.eventQueue.poll();
    }

    public void clearEvents() {
        this.eventQueue.clear();
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            this.resetDeltas();
            this.clearEvents();
        }
    }

    public void setGameFocused(boolean gameFocused) {
        this.gameFocused = gameFocused;
    }
}
